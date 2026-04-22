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
    RollbackService.kt          # the rewind-and-replay engine
  port/
    input/
      NetworkInputPort.kt       # + drainRemoteInputs()
      FramedInput.kt            # (frame, InputState) DTO
    output/
      NetworkOutputPort.kt      # + sendInputWindow(window)
infrastructure/adapter/network/
  NetworkAdapter.kt             # implements drainRemoteInputs via ConcurrentLinkedQueue
```

`GameLoop` shrank to "poll input → `rollback.tick(input)` → render". All state for rollback lives in `RollbackService`.

### RollbackService per-tick algorithm

Given the engine's current frame `N`:

1. **Schedule local input** for frame `N + INPUT_DELAY`. This is GGPO-style input delay: with `INPUT_DELAY = 2`, the peer usually has our input before it's needed and no rollback occurs at all.
2. **Drain incoming authoritative remote inputs** (non-blocking). Dedupe against what we already have (the send window ensures duplicates are normal). For each authoritative input that disagrees with the prediction we used at a past frame `K < N`, schedule a rewind to the earliest such `K`.
3. **If a rewind is scheduled**: restore the snapshot at the start of frame `K`, then replay frames `K..N-1` using stored local inputs (always authoritative on our side) and the newly-corrected remote inputs, re-snapshotting along the way.
4. **Advance one frame**: pick local input for `N` (or `NONE` if none scheduled yet), predict remote input (repeat last known), snapshot the pre-step `World`, call `GameLogic.step`.
5. **Broadcast** a redundant window of the most recent local inputs — a single packet loss is transparently covered by the next one.
6. **Compact** state older than `N - MAX_ROLLBACK_FRAMES`; we'll never roll back that far.

### Tuning knobs (`RollbackConfig`)

| Knob | Default | Meaning |
|---|---|---|
| `inputDelay` | 2 frames (~33 ms) | Pre-apply delay to hide small jitter without any rollback at all. |
| `maxRollbackFrames` | 8 frames (~133 ms) | Hard cap on rewind distance. Beyond this the peer has genuinely diverged and would need a full resync (not implemented; we clamp and let the visual pop through). |
| `sendWindow` | 8 frames | Number of recent local inputs included in each outbound packet for redundancy. |

### Prediction strategy

We use **repeat-last-input**. It's the simplest prediction and, empirically, the single most effective strategy for fighting games: holding a direction for N frames is the common case, not pressing random buttons on every frame. More sophisticated predictors (e.g. based on `InputBuffer` patterns) could be added later without changing the rollback machinery.

## Transport

The wire protocol did **not** need to change. `PeerConnectionBridge.sendInput(mask, frameNumber)` still sends a single (mask, frame) tuple. The redundant send window is implemented by calling this once per frame for each of the last `sendWindow` frames — duplicate arrivals at the remote are deduped in `RollbackService.processIncomingRemote`. This keeps the C++ / JNI bridge untouched.

The `NetworkAdapter` now routes received inputs into two places:

- the legacy per-frame `ConcurrentHashMap` (so `pollRemoteInput` still works for any call sites we haven't migrated),
- a `ConcurrentLinkedQueue<FramedInput>` that `drainRemoteInputs()` empties once per tick (the rollback path).

Busy-waiting in `GameLoop` is gone. The loop always advances.

## Verification

Two Kotest specs cover this:

### `DeterminismSpec`

Runs `GameLogic.step` twice over the same 600- and 3600-frame scripted input sequence and asserts per-frame `World.hashCode()` matches exactly. Any future change that introduces non-determinism (an accidental `Math.random`, a `Instant.now()` call in physics, a `HashSet` iteration) will fail this spec.

### `RollbackServiceSpec`

Builds a fake `DelayedRemote` transport that simulates the peer's own identical `RollbackService` — it applies `inputDelay` symmetrically on the peer side, then adds a configurable `networkDelay` on top. Two properties are checked:

1. **Zero-delay correctness** — with `networkDelay = 0`, `RollbackService` produces bit-identical final state to a zero-latency reference run (`referenceRun` applies `GameLogic.step` directly with `inputDelay` scheduling).
2. **Convergence under delay** — sweeping `networkDelay ∈ {1, 2, 4, 6}`, after enough frames for the final authoritative packets to arrive, the `RollbackService` run still reaches the same position, health, and overall `World` as the reference. This exercises many rewind-and-replay events.

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
| Non-blocking input port | `application/port/input/NetworkInputPort.kt`, `application/port/input/FramedInput.kt` |
| Redundant send port | `application/port/output/NetworkOutputPort.kt` |
| Queue for drain | `infrastructure/adapter/network/NetworkAdapter.kt` |
| Drop the busy-wait | `application/service/GameLoop.kt` |
| Determinism regression | `test/.../DeterminismSpec.kt` |
| Rollback correctness | `test/.../RollbackServiceSpec.kt` |

## Known limits / future work

- **No desync recovery**. If a rollback of more than `maxRollbackFrames` is needed, we clamp silently. A full "resync via state hash comparison and authoritative world retransmission" is not implemented — acceptable for a two-player game, but a real shipping title would include it.
- **No adaptive input delay**. Some rollback implementations (GGPO) bump `inputDelay` up when rollbacks get frequent and back down when the connection is smooth. We keep it fixed.
- **No input-hash exchange**. As a belt-and-braces check against determinism bugs, peers could exchange per-frame `World` hashes and detect drift early. Worth adding if a real desync ever happens in practice.
- **Character data loaded per-client**. Both peers read `Character`/`Attack` tables from the same JSON file. If these files ever diverge (mod mismatch, version skew), the sim desyncs silently. Hashing the character tables at session start and refusing to connect on mismatch would fix this.
