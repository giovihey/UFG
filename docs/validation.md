# Validation

## Strategy

Testing focuses on the **domain layer** — the game logic that must be deterministic and correct. Infrastructure (GUI, network) is validated manually since it depends on external systems.

| Level | Target | Framework |
|-------|--------|-----------|
| Unit tests | Domain systems, components, entities | Kotest (StringSpec) + JUnit5 |
| Static analysis | Code smells, complexity | detekt |
| Formatting | Consistent style | ktlint |
| Manual testing | UI rendering, network play | Visual inspection during `./gradlew run` |

## Test Coverage

### Domain Systems

| Spec | Tests | What it covers |
|------|-------|---------------|
| `InputSystemSpec` | 13 | Horizontal movement, jump (grounded only), no double jump, diagonal input, LEFT+RIGHT priority, missing player, position untouched |
| `PhysicsSystemSpec` | 11 | Single/multi player, gravity, max fall speed, floor collision, stage boundary clamping, game status check |
| `AttackSystemSpec` | 6 | Frame advance, phase transitions (STARTUP→ACTIVE→RECOVERY), attack expiration, no update when paused |
| `HitDetectionSystemSpec` | 6 | No hit in STARTUP/RECOVERY, hit registers in ACTIVE, out-of-range miss, no double hit (hasLanded), hasLanded flag set |
| `PlayerStateSpec` | 11 | IDLE/WALKING/JUMPING state determination, floor reset, ATTACKING state when attackState present, attack expiration returns IDLE |
| `RectangleSpec` | 4 | AABB overlap true, separate false, edge-touching false, corner-touching false |
| `NetworkAdapterSpec` | 3 | Poll returns received input, poll returns null for missing frame, send delegates to bridge |

**Total**: ~54 test cases covering all domain systems.

### What's NOT Tested Automatically

- **Rendering correctness** — visual output verified manually
- **WebRTC connection** — depends on native library and network. Validated via C++ local test (`local_test.cpp`) and manual two-machine play
- **Hitstun/knockback behavior** — functional but test specs not yet written (added in latest changes)
- **Round timer countdown** — functional but test spec not yet written

## Quality Gates

Every build runs:

1. `detekt` — static analysis (fails on weighted issues)
2. `ktlint` — formatting check
3. `compileKotlin` — compiler with `allWarningsAsErrors = true`
4. `test` — all Kotest specs

All four must pass for `./gradlew build` to succeed.

## Acceptance Testing

| Requirement | How validated |
|------------|--------------|
| FR1: Combat | Run game, press P → JAB executes with correct frame timing |
| FR2: Hit detection | Hit opponent → HP decreases by attack damage |
| FR3: Hitstun/knockback | Hit opponent → they slide back and can't act for ~12 frames |
| FR4: Round timer | Timer counts from 99 to 0, round ends at 0 |
| FR5: P2P input sharing | Two machines, both see identical game states |
| FR6: NAT traversal | Two players behind different routers connect via signaling server |
| FR7: Visual feedback | Resize window → game area scales proportionally |
