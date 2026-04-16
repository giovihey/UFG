# Deployment

## Prerequisites

| Requirement | Version |
|------------|---------|
| **JDK** | 17+ |
| **Gradle** | Bundled via `gradlew` wrapper |
| **Native library** | `libwebrtc_wrapper` compiled for your OS (macOS `.dylib`, Linux `.so`, Windows `.dll`) |

## Build from Source

```bash
git clone https://github.com/giovihey/UFG.git
cd UFG/game
./gradlew build
```

This compiles Kotlin, runs detekt + ktlint + all tests.

## Run

### Local Development (Single Player)

```bash
cd game
./gradlew run
```

The game window opens at 800x600. Player 1 is controllable. Without a network peer, Player 2 won't receive input (the game loop will stall waiting for remote input unless running in local-only mode).

### Multiplayer (Two Machines)

**Step 1** — Start the signaling server (Go WebSocket relay):

```bash
# On a machine reachable by both players
cd signaling-server
go run .
```

Default address: `ws://<server-ip>:8080/ws`

**Step 2** — Player A (host):

```bash
cd game
./gradlew run --args="--host"
```

**Step 3** — Player B (joiner):

```bash
cd game
./gradlew run
```

Both clients connect to the signaling server, exchange SDP/ICE, establish P2P, and the match begins.

## Native Library

The WebRTC bridge requires a compiled native library (`libwebrtc_wrapper`). It must be on the JVM's library path:

```bash
# macOS
export DYLD_LIBRARY_PATH=/path/to/native/lib:$DYLD_LIBRARY_PATH

# Linux
export LD_LIBRARY_PATH=/path/to/native/lib:$LD_LIBRARY_PATH

# Windows
# Add the DLL directory to PATH
```

## Useful Commands

```bash
./gradlew test                          # Run tests only
./gradlew test --tests "*.RectangleSpec"  # Single test class
./gradlew ktlintFormat                  # Auto-fix formatting
./gradlew detekt                        # Static analysis
./gradlew dokkaGenerateHtml             # API docs
./gradlew jacocoTestReport              # Coverage report → build/reports/jacoco/
```
