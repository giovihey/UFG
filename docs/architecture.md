# Architecture

UFG uses **Hexagonal Architecture** (Ports & Adapters) to isolate game logic from UI, networking, and input devices. The domain depends on nothing; everything else depends on the domain.

## Why Hexagonal?

A fighting game needs three things that hexagonal architecture provides:

- **Determinism** — pure domain logic with no external dependencies produces identical results given identical inputs. Required for lockstep networking.
- **Testability** — game systems are pure functions testable without a window, network, or OS.
- **Swappable infrastructure** — replace the renderer (Compose → OpenGL) or transport (WebRTC → local loopback) without touching game logic.

## Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                        │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────┐  │
│  │ ComposeAdapter │  │ NetworkAdapter │  │ WebRtcBri │  │
│  │ (GUI + Input)  │  │ (Net I/O)     │  │  dge(JNI) │  │
│  └───────┬────────┘  └───────┬────────┘  └───────────┘  │
│          │                   │                           │
├──────────┼───────────────────┼───────────────────────────┤
│          │      APPLICATION  │                           │
│          ▼                   ▼                           │
│  ┌──────────────────────────────────────────┐            │
│  │            Ports (Interfaces)            │            │
│  │  KeyboardInputPort  │  RenderPort       │            │
│  │  NetworkInputPort   │  NetworkOutputPort │            │
│  └──────────────────────────────────────────┘            │
│                       │                                  │
│  ┌────────────────────┼─────────────────────┐            │
│  │  GameLoop · GameEngine · TimeManager     │            │
│  └────────────────────┬─────────────────────┘            │
│                       │                                  │
├───────────────────────┼──────────────────────────────────┤
│                       ▼                                  │
│                     DOMAIN                               │
│  ┌──────────────────────────────────────────┐            │
│  │  Systems: GameLogic, PhysicsSystem,      │            │
│  │    InputSystem, AttackSystem,            │            │
│  │    HitDetectionSystem                    │            │
│  ├──────────────────────────────────────────┤            │
│  │  Entities: World, Player, Character      │            │
│  ├──────────────────────────────────────────┤            │
│  │  Components: Position, Health, Attack,   │            │
│  │    InputState, Rectangle, Movement ...   │            │
│  ├──────────────────────────────────────────┤            │
│  │  Config: GameConstants                   │            │
│  └──────────────────────────────────────────┘            │
└──────────────────────────────────────────────────────────┘
```

**Dependency rule**: Domain ← Application ← Infrastructure. Inner layers never reference outer layers.

## Domain Layer

**Package**: `domain/`

Pure Kotlin. No framework imports, no I/O, no side effects.

**Systems** are stateless functions: `(World, ...) → World`

| System | Responsibility |
|--------|---------------|
| `GameLogic` | Orchestrator — calls all systems in order, increments frame, applies game rules |
| `InputSystem` | Reads `InputState` bitmask → updates player movement, jump, attack. Blocks input during hitstun |
| `AttackSystem` | Advances attack frame counter, clears expired attacks |
| `PhysicsSystem` | Gravity, floor collision, stage bounds, knockback friction |
| `HitDetectionSystem` | Hitbox/hurtbox overlap → damage, hitstun, knockback |

**Entities**: `World` (immutable game snapshot), `Player` (fighter instance), `Character` (fighter template)

**Components**: small value objects — `Position`, `Health`, `Attack`, `Rectangle`, `InputState`, `Movement`, `AttackState`, `PlayerPhysicsState`

**Config**: `GameConstants` — all tuning values in one place (speeds, gravity, stage dimensions, FPS)

## Application Layer

**Package**: `application/`

Defines **ports** (interfaces) and **services** that wire the game loop together.

### Ports

```kotlin
// Driving — world pushes data out
interface RenderPort {
    fun render(world: World)
    fun shutdown()
}
interface NetworkOutputPort {
    fun sendInput(inputState: InputState, frameNumber: Long)
}

// Driven — outside world sends data in
interface KeyboardInputPort {
    fun press(button: GameButton)
    fun release(button: GameButton)
    fun pollInputState(player: Int): InputState
}
interface NetworkInputPort {
    fun pollRemoteInput(frameNumber: Long): InputState?
}
```

### Services

| Service | What it does |
|---------|-------------|
| `GameLoop` | Main tick: poll time → poll input → send input → receive remote input → step engine → render. Runs on a daemon thread |
| `GameEngine` | Holds the current `World`, delegates `step()` to `GameLogic`. Stateful wrapper around pure domain |
| `TimeManager` | Fixed-timestep accumulator. Physics always advances at 60 Hz regardless of actual frame rate |

`GameLoop` depends only on port interfaces — it never knows which adapter is behind them.

## Infrastructure Layer

**Package**: `infrastructure/`

Concrete implementations of ports. Each adapter translates between an external technology and the port interface.

### ComposeAdapter

Implements **two ports**:

- `KeyboardInputPort` — captures key events from Compose Desktop, maintains a bitmask of pressed keys
- `RenderPort` — holds a `MutableState<World>` that Compose observes. Writing a new `World` triggers recomposition

The UI composables:

- `gameScreen()` — top-level Box layering stage + HUD
- `stageCanvas()` — Canvas that scales game coordinates to window size, draws stage/floor/players/hitboxes
- `hud()` — health bars and round timer overlay

### NetworkAdapter

Implements `NetworkInputPort` + `NetworkOutputPort`:

- **Sending**: forwards input bitmask + frame number to native WebRTC via `WebRtcBridge`
- **Receiving**: JNI callback `onRemoteInput()` stores input in `ConcurrentHashMap<Long, InputState>`. Game loop calls `pollRemoteInput(frame)` to consume it

### WebRtcBridge

JNI wrapper around a C++ WebRTC library (`libdatachannel`). Exposes signaling operations (offer, answer, ICE) and data channel operations (send input).

## Wiring (Composition Root)

In `Main.kt`, all dependencies are assembled via constructor injection:

```kotlin
val networkAdapter = NetworkAdapter(bridge)     // implements NetworkPort
val composeAdapter = ComposeAdapter()           // implements KeyboardInputPort + RenderPort
val engine         = GameEngine(initialWorld)
val timeManager    = TimeManager()
val gameLoop       = GameLoop(engine, composeAdapter, composeAdapter, networkAdapter, timeManager)
```

Swapping a renderer or network transport is a one-line change at this composition root.

## Directory Reference

```
game/src/main/kotlin/com/heyteam/ufg/
├── domain/
│   ├── component/     # Value objects: Position, Health, InputState, Attack ...
│   ├── entity/        # World, Player, Character
│   ├── system/        # GameLogic, InputSystem, PhysicsSystem, AttackSystem, HitDetectionSystem
│   └── config/        # GameConstants
├── application/
│   ├── port/
│   │   ├── input/     # KeyboardInputPort, NetworkInputPort
│   │   └── output/    # RenderPort, NetworkOutputPort
│   └── service/       # GameLoop, GameEngine, TimeManager
├── infrastructure/
│   └── adapter/
│       ├── gui/       # ComposeAdapter + screen/ (GameScreen, StageCanvas, Hud)
│       └── network/   # NetworkAdapter, WebRtcBridge, SignalingClient
└── Main.kt            # Composition root
```
