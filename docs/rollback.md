# Rollback Netcode

## Why Rollback

UFG's previous netcode was **lockstep with blocking delay**: on every frame, the game loop would busy-wait on `NetworkInputPort.pollRemoteInput(frame)` until the peer's input arrived. At 16 ms of ping the game spent most of its wall-clock time waiting, and any single dropped datachannel packet stalled both clients until the packet was replaced.

Rollback netcode replaces waiting with **prediction**:

1. **Predict** the remote input (we repeat the last input we saw — fighters rarely change buttons every frame).
2. **Simulate** forward using the predicted input; show the player immediate feedback.
3. When an authoritative input arrives for a past frame that **disagrees** with our prediction, **restore the pre-frame snapshot** and **replay** forward to the present using the corrected input.

The visible consequence to the player is short, localized "pops" on mispredictions instead of global stuttering.

## Prerequisite: Determinism

Rollback only works if the simulation is **bit-identical** across machines given identical inputs. Otherwise replaying `(world, input)` on the remote and on us produces different results and rollback "corrects" into divergence.

### What the codebase already had going for it

- **No RNG** anywhere in `domain/` or `application/`.
- **No transcendentals** — no `Math.sqrt`, `Math.sin/cos`, `Math.pow`. Only `+ - * /` on `Double`, which is bit-identical across x86 and ARM JVMs under IEEE 754.
- **Single-threaded simulation** — the only concurrent touchpoint is the network input queue.
- **Immutable data classes all the way down** (`World`, `Player`, `Movement`, `Position`, `Rectangle`, `Health`, `AttackState`, `PlayerPhysicsState`). This makes snapshot/restore free — see below.

### What we had to fix

1. **Wall-clock `dt`**. `TimeManager` fed a wall-clock-derived `fixedDt` into `GameLogic.step`. Even a few hundred microseconds of drift per tick accumulates into different positions across peers. We removed the `dt` parameter from `GameLogic.step` and `PhysicsSystem.update` entirely; physics now reads the constant `GameConstants.FRAME_DT = 1.0 / 60.0`. `TimeManager` still paces ticks (when to step), but never decides step magnitude (how far to advance).
2. **Map iteration order**. `World.players` is a `Map<Int, Player>`. A hypothetical JVM version that rehashed the underlying `HashMap` differently on one peer would silently flip cross-player iteration and desync collision resolution. `HitDetectionSystem.update` now iterates over `players.keys.sorted()` for a guaranteed-stable order.
3. **Determinism regression test**. `DeterminismSpec` runs `GameLogic.step` twice over a fixed 3600-frame scripted input sequence and asserts bit-identical per-frame `World.hashCode()`. This runs on every build and catches any accidentally-introduced non-determinism.

## Snapshots for free

Every type in the domain is an immutable `data class` containing only:

- primitives and enums, or
- references to other immutable data classes, or
- the shared immutable `Character` reference.

Kotlin's `Map<K, V> + (k to v)` operator returns a fresh `LinkedHashMap` rather than mutating in place, and `data class` `copy()` cascades all the way down. **A `World` reference is itself a snapshot.** To "restore" we reassign the engine's `world` field to the remembered reference; no deep copy is needed.

This is the main reason rollback was practical to add in a weekend. See `GameEngine.setWorld` and the ring buffer in `RollbackService`.

## Architecture

Rollback is **application-layer orchestration**. The domain (`GameLogic`, all `*System` objects, `World`, `Player`, etc.) stays synchronous and unaware of frames-in-flight. The new component is:

```
application/
  service/
    RollbackService.kt          # the rewind-and-replay engine + time-sync stall
    RollbackListener.kt         # observer hook (onPredictionEvaluated/onRollback/onStall/...)
    NetcodeEventLogger.kt       # one [RB] line per rewind
    NetcodeStatsLogger.kt       # per-second [NETCODE] summary (rb/s, stalls/s, advantage)
  port/
    input/
      NetworkInputPort.kt       # + drainRemoteInputs() + peerFrame()
      FramedInput.kt            # (frame, InputState) DTO
    output/
      NetworkOutputPort.kt      # + sendInputWindow(senderCurrentFrame, window)
infrastructure/adapter/network/
  NetworkAdapter.kt             # ConcurrentLinkedQueue + AtomicLong peerLatestSenderFrame
```

`GameLoop` shrank to "poll input → `rollback.tick(input)` → render". All state for rollback lives in `RollbackService`.

### RollbackService per-tick algorithm

Given the engine's current frame `N`:

0. **Time-sync check**. Read `peerFrame()` from the network port (the highest live sim frame the peer has reported on its packets). If `N - peerFrame > syncThreshold`, **stall this tick**: drain incoming packets so corrections still land, fire `listener.onStall`, and return without advancing. We don't sample local input or broadcast on a stalled tick. This caps `currentFrame - peerFrame` so authoritative inputs never arrive past the snapshot ring.
1. **Schedule local input** for frame `N + INPUT_DELAY`. This is GGPO-style input delay: with `INPUT_DELAY = 2`, the peer usually has our input before it's needed and no rollback occurs at all.
2. **Drain incoming authoritative remote inputs** (non-blocking). Dedupe against what we already have (the send window ensures duplicates are normal). For each authoritative input that disagrees with the prediction we used at a past frame `K < N`, schedule a rewind to the earliest such `K`.
3. **If a rewind is scheduled**: restore the snapshot at the start of frame `K`, then replay frames `K..N-1` using stored local inputs (always authoritative on our side) and the newly-corrected remote inputs, re-snapshotting along the way.
4. **Advance one frame**: pick local input for `N` (or `NONE` if none scheduled yet), predict remote input (repeat last known), snapshot the pre-step `World`, call `GameLogic.step`.
5. **Broadcast** a redundant window of the most recent local inputs alongside our live frame `N` — a single packet loss is transparently covered by the next one, and the piggybacked `senderCurrentFrame` is what the *peer's* step 0 will read on the next tick.
6. **Compact** state older than `N - MAX_ROLLBACK_FRAMES`; we'll never roll back that far.

### Why time-sync matters

Without step 0 the local clock can drift ahead of the peer's indefinitely (different machine speeds, GC pauses, OS scheduling). Once `currentFrame - peerFrame` exceeds `maxRollbackFrames`, late authoritative inputs land outside the snapshot window and are silently clamped — a permanent desync with no listener event. Step 0 keeps the gap bounded to roughly `syncThreshold + 1` frames in steady state by stalling the side that's ahead. Both peers run the same logic, so only the side that is actually ahead pays a stall.

The current implementation is **one-sided**: each side compares its frame to the peer's reported frame without subtracting one-way latency. This biases both sides slightly toward thinking they're ahead, so they may stall a hair more than the GGPO ideal. The fix (echo the peer's observed advantage back so each side can subtract the bias) is a small follow-up — see future work below.

### Tuning knobs (`RollbackConfig`)

| Knob | Default | Meaning |
|---|---|---|
| `inputDelay` | 2 frames (~33 ms) | Pre-apply delay to hide small jitter without any rollback at all. |
| `maxRollbackFrames` | 8 frames (~133 ms) | Hard cap on rewind distance. Beyond this the peer has genuinely diverged and would need a full resync (not implemented; we clamp and let the visual pop through). |
| `sendWindow` | 8 frames | Number of recent local inputs included in each outbound packet for redundancy. |
| `syncThreshold` | 2 frames | Frame advantage we tolerate over the peer before stalling a tick. Must be strictly less than `maxRollbackFrames`; an `init` guard enforces this. |
| `committedHashRetention` | 60 frames | How many frames of committed-frame hashes we keep locally for cross-peer desync comparison. Must be strictly greater than `maxRollbackFrames`; an `init` guard enforces this. |

### Prediction strategy

We use **repeat-last-input**. It's the simplest prediction and, empirically, the single most effective strategy for fighting games: holding a direction for N frames is the common case, not pressing random buttons on every frame. More sophisticated predictors (e.g. based on `InputBuffer` patterns) could be added later without changing the rollback machinery.

## Transport

`PeerConnectionBridge.sendInput(inputMask, frameNumber, senderCurrentFrame, committedFrame, committedHash)` carries five values per packet:

- `inputMask` — the input bitmask;
- `frameNumber` — the frame the input belongs to;
- `senderCurrentFrame` — the sender's live sim frame at the moment of the send (time-sync stalling);
- `committedFrame` — the frame `committedHash` is over, or `Long.MIN_VALUE` if the sender has no committed hash yet;
- `committedHash` — canonical [`WorldHash`](#desync-detection) of the sender's `committedFrame`; ignored when `committedFrame == Long.MIN_VALUE`.

The redundant send window is implemented by calling this once per frame for each of the last `sendWindow` frames — duplicate arrivals at the remote are deduped in `RollbackService.processIncomingRemote` (and in `NetworkAdapter` for the committed-hash piggyback).

The wire format is **36 bytes**: 4 (`int32` mask) + 8 (`int64` frameNumber) + 8 (`int64` senderCurrentFrame) + 8 (`int64` committedFrame) + 8 (`int64` committedHash). The C++ wrapper (`channel/src/webrtc_wrapper.cpp`) packs and unpacks these with `memcpy`; both peers must run a wrapper built from the matching header — a sender packing fewer bytes against a receiver expecting 36 (or vice versa) will scramble frame numbers silently. Rebuild after any wire-format change with `cd channel && cmake --build build`.

The `NetworkAdapter` routes received packets into four places:

- the legacy per-frame `ConcurrentHashMap` (so `pollRemoteInput` still works for any call sites we haven't migrated),
- a `ConcurrentLinkedQueue<FramedInput>` that `drainRemoteInputs()` empties once per tick (the rollback path),
- an `AtomicLong` tracking the highest `senderCurrentFrame` ever reported, exposed as `peerFrame()` for the time-sync check. Updates are monotonic — out-of-order packets carrying older sender frames are ignored,
- a `ConcurrentLinkedQueue<RemoteCommittedHash>` for committed-frame hash piggybacks, with frame-keyed deduplication so the redundant send window doesn't flood the rollback service with duplicates.

Busy-waiting in `GameLoop` is gone. The loop always advances (modulo time-sync stalls).

## Desync detection

Time-sync stalling keeps `currentFrame - peerFrame` bounded *if both peers are deterministic*. To detect the case where they aren't — a determinism bug, a clamped correction, or game-data mismatch on the two clients — we cross-compare hashes of "committed" world states.

A frame `F` is **committed** once it has fallen out of the rollback window (`F < currentFrame - maxRollbackFrames`). At that point no further authoritative input can change the world we recorded for it, so both peers' worlds for frame `F` should be byte-identical. If they aren't, that's a confirmed desync.

The mechanism:

1. In `compact()`, just before discarding a snapshot whose frame is now committed, we run [`WorldHash.hash()`](#canonical-hash) on it and store the result in `committedHashes[frame]`. The most recent committed frame's hash also becomes our outbound piggyback.
2. Every outbound packet carries `(committedFrame, committedHash)`. On receive, the adapter dedupes by frame and exposes the queue via `drainRemoteCommittedHashes()`.
3. Each tick (whether or not we stalled), `RollbackService.checkDesyncs` drains the queue and looks up our own hash for each peer-reported frame. On mismatch, it fires `listener.onDesync(DesyncEvent(...))`.

We retain committed hashes for `committedHashRetention = 60` frames so a peer running slightly behind us still has a window in which our hash is available for comparison.

There is **no automatic recovery**. The current implementation logs and continues — a real shipping title would freeze the sim and request authoritative state retransmission. This is documented as a follow-up in [Future Works](future_works.md#tier-1--finishing-what-we-started).

### Canonical hash

`WorldHash` is a 64-bit FNV-1a hash over an explicitly-chosen subset of `World`:

- `frameNumber`, `gameStatus.ordinal`, `roundTimer`;
- per player (iterated in `players.keys.sorted()` order to be insertion-order-independent): `id`, `position.{x,y}`, `health.current`, all `physicsState` fields, and an `attackState` block (presence flag + `attack.name` + `currentFrame` + `hasLanded`).

It is **not** `World.hashCode()`. Auto-generated `hashCode` includes every field — adding a purely-cosmetic field tomorrow would change the hash and produce false desync alarms. By picking the fields explicitly we control exactly what counts as "the sim's true state".

`Double` values are fed through `toRawBits()` for IEEE-754-deterministic hashing — same constraint we already rely on for sim determinism, so no new requirement.

## Verification

Two Kotest specs cover this:

### `DeterminismSpec`

Runs `GameLogic.step` twice over the same 600- and 3600-frame scripted input sequence and asserts per-frame `World.hashCode()` matches exactly. Any future change that introduces non-determinism (an accidental `Math.random`, a `Instant.now()` call in physics, a `HashSet` iteration) will fail this spec.

### `RollbackServiceSpec`

Builds a fake `DelayedRemote` transport that simulates the peer's own identical `RollbackService` — it applies `inputDelay` symmetrically on the peer side, then adds a configurable `networkDelay` on top. Five properties are checked:

1. **Zero-delay correctness** — with `networkDelay = 0`, `RollbackService` produces bit-identical final state to a zero-latency reference run (`referenceRun` applies `GameLogic.step` directly with `inputDelay` scheduling).
2. **Convergence under delay** — sweeping `networkDelay ∈ {1, 2, 4, 6}`, after enough frames for the final authoritative packets to arrive, the `RollbackService` run still reaches the same position, health, and overall `World` as the reference. This exercises many rewind-and-replay events.
3. **Stalls when ahead** — with the peer's `peerFrame()` pinned at 0 and `syncThreshold = 2`, the service advances exactly `syncThreshold + 1` frames and then stalls every subsequent tick, with `onStall` fired the expected number of times.
4. **Bounded advantage** — against a peer that reports a constant 5-frame deficit, after 200 ticks the local frame number minus `peerFrame()` stays within `syncThreshold + 1`. Without step 0 this gap would grow without bound.
5. **Desync detection** — after running long enough to commit a hash, injecting a peer hash that disagrees with our own for the same frame produces exactly one `onDesync` event with the right fields; subsequently injecting a *matching* hash for a later frame fires nothing.

### `WorldHashSpec`

Direct properties of the canonical hash:

- determinism (same world ⇒ same hash);
- sensitivity to single-ULP position changes, frame-number changes, and health changes;
- insensitivity to map insertion order (sorted iteration);
- two parallel `GameLogic.step` runs over the same input script produce identical hashes at every frame — the cross-peer guarantee, expressed locally.

### `NetworkAdapterSpec`

Checks that `peerFrame()` tracks the maximum `senderCurrentFrame` and never regresses on out-of-order delivery, and that `drainRemoteCommittedHashes()` dedupes by frame and ignores the `Long.MIN_VALUE` sentinel.

Run with:

```bash
./gradlew test --tests "com.heyteam.ufg.application.service.DeterminismSpec"
./gradlew test --tests "com.heyteam.ufg.application.service.RollbackServiceSpec"
```

End-to-end (two local instances over loopback WebRTC) is manual:

```bash
./gradlew run  # twice, one host, one client
```

Expect responsive local input (only `INPUT_DELAY` frames of perceived lag on the local side) and no visible warping under moderate simulated latency.

## Files

| Change | File |
|---|---|
| Constant 60 Hz step | `domain/config/GameConstants.kt` (`FRAME_DT`) |
| Remove `dt` from step | `domain/system/GameLogic.kt`, `domain/system/PhysicsSystem.kt` |
| Deterministic iteration | `domain/system/HitDetectionSystem.kt` |
| Snapshot restore hook | `application/service/GameEngine.kt` (`setWorld`) |
| Rewind-and-replay engine | `application/service/RollbackService.kt` (new) |
| Non-blocking input port + `peerFrame()` | `application/port/input/NetworkInputPort.kt`, `application/port/input/FramedInput.kt` |
| Redundant send port + `senderCurrentFrame` | `application/port/output/NetworkOutputPort.kt` |
| Queue for drain + peer-frame tracker | `infrastructure/adapter/network/NetworkAdapter.kt` |
| Wire format (36 bytes) | `channel/include/webrtc_wrapper.h`, `channel/src/webrtc_wrapper.cpp` |
| Canonical hash | `application/service/WorldHash.kt` (new) |
| Desync detection | `application/service/RollbackService.kt` (`compact`, `checkDesyncs`), `RollbackListener.kt` (`DesyncEvent`, `onDesync`), `NetcodeEventLogger.kt` (`[DESYNC]` line) |
| Canonical hash spec | `test/.../WorldHashSpec.kt` (new) |
| JNI bridge declaration | `infrastructure/adapter/network/WebRtcBridge.kt`, `PeerConnectionBridge.kt`, `DataChannelListener.kt` |
| Drop the busy-wait | `application/service/GameLoop.kt` |
| Listener hook (`onStall`) + stats columns | `application/service/RollbackListener.kt`, `NetcodeStatsLogger.kt` |
| Determinism regression | `test/.../DeterminismSpec.kt` |
| Rollback correctness + stall specs | `test/.../RollbackServiceSpec.kt` |
| Peer-frame tracking spec | `test/.../NetworkAdapterSpec.kt` |

## Known limits / future work

- **One-sided time sync**. Step 0 stalls when *we* think we're ahead, without subtracting one-way latency. Both sides slightly over-estimate their lead, so each may stall a hair more than necessary. The GGPO-style fix is to echo the peer's observed advantage back on every packet (one extra `Long` on the wire) and subtract it before comparing — small change once the wire format is in place.
- **Desync is detected, not recovered**. We now fire `onDesync` when a determinism bug or game-data mismatch causes the two sims to diverge, but we don't *fix* it. A real shipping title would freeze the sim and request authoritative state retransmission. Today the listener just logs an `[DESYNC]` error.
- **No adaptive input delay**. Some rollback implementations (GGPO) bump `inputDelay` up when rollbacks get frequent and back down when the connection is smooth. We keep it fixed.
- **Character data loaded per-client**. Both peers read `Character`/`Attack` tables from the same JSON file. The `WorldHash` includes `attack.name` so a mismatched move *will* be detected at the first hit, but ideally we'd hash the character tables at session start and refuse to connect on mismatch instead of letting the match start and then desyncing.
