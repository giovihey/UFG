# Future Works

## Rollback Netcode — **Done (core)**

Implemented in `application/service/RollbackService.kt`, including one-sided time-sync stalling so the local clock can't drift past `maxRollbackFrames` ahead of the peer. See [Rollback Netcode](rollback.md) for the full write-up. Open follow-ups are listed below, ranked roughly by impact-per-effort.

### Tier 1 — finishing what we started

- **State-hash desync detection — Done.** Each peer canonical-hashes its committed-frame `World` and piggybacks `(committedFrame, committedHash)` on every input packet; receiver compares against its own hash and fires `onDesync` on mismatch. See `WorldHash.kt` and the desync block in `RollbackService.kt`. Open follow-up: **automatic recovery** (currently we only log — a real shipping title would freeze and request authoritative state retransmission).
- **Two-sided time sync.** Each packet additionally echoes back the peer's last-known advantage as we observed it. Each side then computes `myAdv - peerAdv` and only the side with the bigger gap stalls. Removes the one-way-latency bias that makes both sides slightly over-stall today. Tiny change — wire format already has room to grow.
- **Silent-clamp observability.** Today, if a misprediction arrives older than `maxRollbackFrames`, `resimulateFrom` clamps and replays from a wrong starting frame with no listener event. Time-sync should prevent this in practice, but we want to *know* if it ever fires. Add `listener.onClampedCorrection(...)` and a stat counter — trivial.
- **Character/asset hash at session start.** The `WorldHash` already includes `attack.name`, so a mismatched move *will* desync at first hit. Better: hash the entire character JSON at handshake and refuse to connect on mismatch, so the match never starts.

### Tier 2 — UX polish

- **Visual rollback smoothing.** On a rewind, interpolate rendered positions over 2–3 render frames instead of teleporting. Doesn't change the simulation. Single biggest perceived-quality improvement available — turns visible "pops" into smooth corrections. Needs a separate "render world" derived from "sim world" with a small lerp.
- **Adaptive input delay.** When rollbacks per second exceed a threshold, bump `inputDelay` from 2 → 3 → 4. When it's been clean for a while, drop it back. Both peers must agree on the new delay (negotiated through the same packet stream) or they desync.
- **Disconnect / freeze handling.** If no packet arrives in N frames (~30), pause the game and show "waiting for opponent…", then resume cleanly when packets resume. Today the loop just keeps predicting "repeat last input" until `onDataChannelClose` fires and stops everything. Turns transient blips into a brief pause instead of an instant loss.

### Tier 3 — debugging & dev experience

- **Input log replay.** Capture every local + remote input to a file. Replay through `RollbackService` later to reproduce a session bit-for-bit. Combined with state hashes, this makes desync bug reports actually debuggable — without it they're nearly impossible.
- **Real-time stats overlay.** Render `NetcodeStatsLogger` numbers as a HUD corner: rollbacks/sec, prediction %, advantage, stalls/sec. Toggleable. FGC players expect this; it also makes net problems immediately visible during testing.
### Tier 4 — bigger projects

- **Spectator mode.** A third client connects, receives both peers' input streams, runs the same `RollbackService`. No special server needed beyond signaling tweaks. Essential for tournament streaming.
- **Save state / training mode.** Snapshot a `World` to disk, restore on demand, scrub frame-by-frame. Determinism + immutable data classes makes the core almost trivial — most of the work is the UI for combo lab / frame-data inspection.

## Blocking Mechanic

Add a **BLOCK** state triggered by holding back (direction away from opponent). On hit while blocking:

- Reduced or zero damage
- Enter BLOCKSTUN instead of HITSTUN (shorter, no knockback or reduced knockback)
- Creates risk/reward: blocking is safe but you can't attack

Requires: opponent-relative direction detection, `BLOCKSTUN` state handling in `InputSystem` and `PhysicsSystem`.

## Expanded Move List

Currently only JAB exists. Planned:

- **Kick** (K button) — different frame data and hitbox
- **Heavy attacks** — slower startup, more damage and hitstun
- **Air attacks** — available only while jumping
- Wire the `Character` entity into game initialization so different fighters can have different movesets

## Character Selection

The `Character` class exists but isn't used. A character select screen would:

- Let each player pick a fighter with different stats and moves
- Load the selected character's `moveList` and `defaultHurtbox` into the `Player` entity

## Visual Improvements

- Replace colored rectangles with sprite animations
- Add hit effects (flash on hit, screen shake)
- Hitstun visual feedback (player flashes or changes color)
- Background art for the stage

## Round / Match System

- Best-of-3 rounds
- Win counter display
- Match result screen
- Rematch option
