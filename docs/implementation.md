# Implementation

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Language | **Kotlin** (JVM) | Course requirement. Cross-platform, expressive, null-safe |
| UI | **Compose Desktop** | Declarative UI for Kotlin. Thread-safe state observation via `mutableStateOf`. No Swing boilerplate |
| Networking | **libdatachannel** (C++) | Lightweight WebRTC — data channels only, ~2 MB. Connected via JNI |
| Signaling | **Go WebSocket server** | Relays SDP/ICE during connection setup. Stateless, minimal |
| Build | **Gradle** (Kotlin DSL) | Standard Kotlin build tool. Manages Compose plugin, detekt, ktlint, Kotest |
| Testing | **Kotest** + JUnit5 | Kotlin-native test framework with expressive spec styles |
| Code quality | **ktlint** + **detekt** | Formatting enforcement + static analysis. Warnings are errors |

## Network Protocol

### Transport: WebRTC Data Channel

- **Underlying protocol**: SCTP over DTLS over UDP
- **Delivery mode**: Unordered, unreliable (mimics raw UDP)
- **Encryption**: DTLS (automatic with WebRTC)
- **Payload**: `InputState` bitmask (`Int`, 4 bytes) + frame number (`Long`, 8 bytes) = 12 bytes per packet

### Signaling: WebSocket + JSON

During connection setup only. Messages:

```json
{"type": "sdp", "sdp": "<SDP text blob>"}
{"type": "ice", "candidate": "<candidate>", "mid": "<media ID>"}
```

The signaling server is a simple Go WebSocket relay. It forwards messages between the two connected clients and is not needed after the P2P connection is established.

## Frame Data System

Attacks are defined by frame counts at 60 FPS:

```
JAB: 4 startup → 3 active → 8 recovery = 15 total frames (250 ms)
     hitstunFrames: 12 (200 ms opponent can't act)
     knockbackSpeed: 150 px/s
     damage: 5
```

| Phase | What happens |
|-------|-------------|
| **Startup** | Attack is committed but hitbox isn't active yet. The attacker is vulnerable |
| **Active** | Hitbox is live. If it overlaps an opponent's hurtbox, the hit registers |
| **Recovery** | Attack is ending. Hitbox gone, attacker can't act yet |

Hitbox position is computed relative to the player's `topLeft` corner during the ACTIVE phase only: `attack.hitBox.copy(x = topLeft.x + offset.x, y = topLeft.y + offset.y)`.

## Physics

All values in `GameConstants`:

| Constant | Value | Unit |
|----------|-------|------|
| `WALK_SPEED` | 120 | px/s |
| `JUMP_INITIAL_VELOCITY` | -350 | px/s (negative = up) |
| `GRAVITY` | 1000 | px/s^2 |
| `MAX_FALL_SPEED` | 500 | px/s |
| `FLOOR_Y` | 320 | px |
| `STAGE_WIDTH` / `HEIGHT` | 800 / 600 | px |
| `STAGE_MARGIN` | 100 | px |
| `KNOCKBACK_FRICTION` | 0.92 | multiplier per frame |
| `TARGET_FPS` | 60 | Hz |

Physics runs at a fixed timestep (`1/60s`) via `TimeManager`'s accumulator — regardless of actual frame rate.

## Rendering

**Compose Desktop Canvas** with coordinate scaling:

```kotlin
val scale = minOf(canvasWidth / STAGE_WIDTH, canvasHeight / STAGE_HEIGHT)
drawContext.transform.translate(offsetX, offsetY)
drawContext.transform.scale(scale, scale, Offset.Zero)
```

The game world (800x600 logical units) is scaled uniformly to fill the window, centered with letterboxing if the aspect ratio doesn't match.

Visual elements:

- **Players**: colored rectangles (Blue = P1, Red = P2) at their hurtbox dimensions
- **Hitboxes**: yellow outline, visible only during ACTIVE attack phase
- **Stage**: dark background, floor line, margin boundaries
- **HUD**: health bars (proportional fill) + round timer (center)

## Build Pipeline

```bash
./gradlew build      # compile + detekt + ktlint + tests
./gradlew run        # launch the game
./gradlew test       # tests only
./gradlew ktlintFormat  # auto-fix formatting
```

Compiler setting: `allWarningsAsErrors = true`. CI runs on GitHub Actions with semantic-release for automated versioning.
