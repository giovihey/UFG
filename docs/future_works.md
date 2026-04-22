# Future Works

## Rollback Netcode — **Done**

Implemented in `application/service/RollbackService.kt`. See [Rollback Netcode](rollback.md) for the full write-up. Remaining follow-ups documented there: full desync recovery, adaptive input delay, periodic state-hash exchange.

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
