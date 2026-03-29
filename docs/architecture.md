# UFG - Architecture Documentation

UFG (Ultra Fighting Game) adopts **Hexagonal Architecture** (also known as **Ports & Adapters**), a pattern introduced by Alistair Cockburn that isolates the core business logic from external concerns such as UI frameworks, networking libraries, and input devices.

This document explains how the architecture is organized, why each layer exists, and how data flows through the system.

---

## Why Hexagonal Architecture?

A fighting game has unusually strict requirements:

- **Determinism** -- given the same inputs, the simulation must produce identical results across machines (critical for netplay).
- **Testability** -- frame-data logic, physics, and hit detection must be verifiable without booting a window or a network stack.
- **Swappable infrastructure** -- the rendering backend (today Compose Desktop, tomorrow perhaps OpenGL) or the network transport (WebRTC, GGPO, local) should change without touching game logic.

Hexagonal Architecture addresses all three by enforcing a strict dependency rule: **the domain depends on nothing; everything else depends on the domain.**

---

## Layer Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Infrastructure                        │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ ComposeAdapter│  │NetworkAdapter│  │  WebRtcBridge │  │
│  │  (GUI + Input)│  │ (Net I/O)   │  │  (JNI/C++)    │  │
│  └──────┬───────┘  └──────┬───────┘  └───────────────┘  │
│         │                  │                              │
├─────────┼──────────────────┼──────────────────────────────┤
│         │    Application   │                              │
│         ▼                  ▼                              │
│  ┌─────────────────────────────────────────────┐         │
│  │              Ports (Interfaces)              │         │
│  │  ┌────────────────┐  ┌────────────────────┐ │         │
│  │  │  Input Ports   │  │   Output Ports     │ │         │
│  │  │ KeyboardInput  │  │   RenderPort       │ │         │
│  │  │ NetworkInput   │  │   NetworkOutput    │ │         │
│  │  └────────────────┘  └────────────────────┘ │         │
│  └─────────────────────────────────────────────┘         │
│                        │                                  │
│  ┌─────────────────────┼───────────────────────┐         │
│  │   Application Services                      │         │
│  │   GameLoop · GameEngine · TimeManager       │         │
│  └─────────────────────┬───────────────────────┘         │
│                        │                                  │
├────────────────────────┼──────────────────────────────────┤
│                        ▼                                  │
│                      Domain                               │
│  ┌─────────────────────────────────────────────┐         │
│  │  Systems: GameLogic, PhysicsSystem,         │         │
│  │           InputSystem, HitDetectionSystem   │         │
│  ├─────────────────────────────────────────────┤         │
│  │  Entities: World, Player, Character         │         │
│  ├─────────────────────────────────────────────┤         │
│  │  Components: Position, Movement, Health,    │         │
│  │  Rectangle, InputState, GameButton, Attack  │         │
│  ├─────────────────────────────────────────────┤         │
│  │  Config: GameConstants                      │         │
│  └─────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## Domain Layer

**Package:** `domain/`

The domain is the innermost ring. It contains all game rules and has **zero dependencies on external frameworks**. Every type here is a pure Kotlin data structure or a pure function.

### Entities

| Class       | Responsibility                                                                 |
|-------------|-------------------------------------------------------------------------------|
| `World`     | Immutable snapshot of the entire game state: players, stage bounds, frame number, round timer, game status. |
| `Player`    | A fighter instance -- position, velocity, health, hurtbox, physics state.      |
| `Character` | A fighter archetype -- name, base health, move list mapping inputs to attacks.  |

`World` is the single source of truth for any given frame. Because it is immutable (`data class` with `copy()`), each simulation step produces a **new** `World` rather than mutating the old one. This makes the game loop inherently deterministic and enables future rollback netcode.

### Components (Value Objects)

Small, reusable data holders that compose into entities:

- **Position / Direction** -- 2D coordinates and normalized direction vectors.
- **Movement** -- bundles direction, position, and speed.
- **Rectangle** -- axis-aligned bounding box with an `overlaps()` method for collision detection.
- **Health** -- current and maximum hit points.
- **InputState** -- a bitmask (value class) of currently pressed `GameButton`s. Checking a button is a single bitwise AND.
- **Attack** -- damage value, hitbox rectangle, and frame data (startup, active, recovery frames).
- **InputBuffer** -- circular queue of 60 `InputState` entries for combo detection.

### Systems

Domain systems are **stateless functions** that take a `World` (plus inputs) and return a new `World`:

| System               | What it does                                                                 |
|----------------------|-----------------------------------------------------------------------------|
| `GameLogic`          | Top-level step orchestrator. Calls the other systems in order, then increments the frame counter. |
| `InputSystem`        | Reads the `InputState` bitmask and updates the player's `Movement` (direction, jump velocity). |
| `PhysicsSystem`      | Applies gravity, enforces terminal velocity, clamps positions to stage bounds, detects ground. |
| `HitDetectionSystem` | Checks hitbox/hurtbox overlap and applies damage. *(Stubbed -- returns world unchanged for now.)* |

A single simulation tick looks like:

```
InputSystem.apply(world, input)
    → PhysicsSystem.update(world, fixedDt)
        → GameLogic.applyGameRules(world)
            → world.copyWithFrameIncrement()
```

### Constants

`GameConstants` centralizes all tuning values (walk speed, gravity, stage dimensions, target FPS) so they are easy to find and adjust.

---

## Application Layer

**Package:** `application/`

The application layer sits between the domain and the outside world. It defines **ports** (interfaces) that external systems must implement, and it contains **services** that orchestrate the game loop.

### Ports

Ports are the hexagonal architecture's primary mechanism for decoupling. They are plain Kotlin interfaces with no implementation details.

#### Input Ports (Driving)

These represent ways the outside world **sends data into** the application:

```kotlin
interface KeyboardInputPort {
    fun press(button: GameButton)
    fun release(button: GameButton)
    fun pollInputState(player: Int): InputState
}

interface NetworkInputPort {
    fun pollRemoteInput(frameNumber: Long): InputState?
}
```

The game loop calls `pollInputState()` each frame to get the local player's input, and `pollRemoteInput()` to get the remote player's input for a specific frame.

#### Output Ports (Driven)

These represent ways the application **pushes data out** to the external world:

```kotlin
interface RenderPort {
    fun render(world: World)
}

interface NetworkOutputPort {
    fun sendInput(inputState: InputState, frameNumber: Long)
}
```

After each simulation step, the game loop calls `render()` to display the updated state. During networked play, it calls `sendInput()` to transmit local input to the remote opponent.

### Application Services

| Service       | Responsibility                                                                                 |
|---------------|-----------------------------------------------------------------------------------------------|
| `GameLoop`    | Main loop. Coordinates timing, input polling, simulation stepping, and rendering. Runs on a daemon thread. |
| `GameEngine`  | Holds the current `World` and delegates each `step()` call to `GameLogic`. Stateful wrapper around the pure domain. |
| `TimeManager` | Implements a fixed-timestep accumulator. Physics always advances at 60 Hz regardless of actual frame rate, preventing inconsistencies across different machines. |

The `GameLoop` depends **only on port interfaces**, never on concrete adapters. This is the inversion of control at the heart of hexagonal architecture.

---

## Infrastructure Layer

**Package:** `infrastructure/`

Infrastructure contains the **adapters** -- concrete implementations of the ports. Each adapter translates between an external technology and the port interface the application expects.

### GUI Adapter: `ComposeAdapter`

`ComposeAdapter` implements **two ports at once**:

- **`KeyboardInputPort`** -- captures AWT key events from the Compose Desktop window and maintains a bitmask of pressed keys. `pollInputState()` returns this bitmask wrapped in an `InputState`.
- **`RenderPort`** -- holds a `MutableState<World>` that Compose observes. When `render(world)` is called from the game thread, Compose automatically recomposes the UI.

The UI is composed of:

- `gameScreen()` -- top-level layout combining the stage and HUD.
- `stageCanvas()` -- draws the floor and each player as a colored rectangle (hurtbox).
- `hud()` -- overlays health bars and the round timer.

**Thread safety:** The game loop writes `World` from its own thread; Compose reads it from the UI thread. Compose's `mutableStateOf` handles synchronization.

### Network Adapter: `NetworkAdapter`

`NetworkAdapter` implements both `NetworkInputPort` and `NetworkOutputPort`:

- **Sending:** `sendInput()` forwards the local input bitmask and frame number to the native WebRTC library via `WebRtcBridge`.
- **Receiving:** When the C++ library receives remote input, it calls back into `onRemoteInput()` via JNI. The adapter stores the input in a `ConcurrentHashMap<Long, InputState>` keyed by frame number. The game loop later calls `pollRemoteInput(frame)` to retrieve and consume it.

### WebRTC Bridge: `WebRtcBridge`

A JNI wrapper around a native C++ WebRTC library (`libwebrtc_wrapper.dylib`). It exposes signaling operations (create offer, set remote description, add ICE candidate) and data channel operations (send input).

---

## Data Flow

### Per-Frame Lifecycle

```
 UI Thread                          Game Thread
 ────────                           ───────────
 Key press event
   │
   ▼
 ComposeAdapter.press()
   │  (updates bitmask)
   │
   │                                TimeManager.update()
   │                                  │
   │                                  ▼
   │                                pollInputState() ──► reads bitmask
   │                                  │
   │                                  ▼
   │                                GameEngine.step(input, fixedDt)
   │                                  │
   │                                  ├─► InputSystem   (input → movement)
   │                                  ├─► PhysicsSystem (gravity, bounds)
   │                                  ├─► GameRules     (round end check)
   │                                  └─► frame++
   │                                  │
   │                                  ▼
   │                                RenderPort.render(world)
   │                                  │
   ▼                                  ▼
 ComposeAdapter.worldState = world
   │
   ▼
 Compose recomposition
   │
   ▼
 stageCanvas() + hud() redraw
```

### Networked Play (Planned)

```
 Local Machine                      Remote Machine
 ─────────────                      ──────────────
 pollInputState()
   │
   ├──► GameEngine.step()
   │
   ├──► NetworkOutputPort
   │      .sendInput(input, frame)
   │          │
   │          ▼
   │    WebRtcBridge.sendInput()
   │          │
   │          ════════════════════► onRemoteInput(mask, frame)
   │                                  │
   │                                  ▼
   │                                ConcurrentHashMap[frame] = input
   │                                  │
   │                                  ▼
   │                                pollRemoteInput(frame)
   │                                  │
   │                                  ▼
   │                                GameEngine.step(remoteInput, fixedDt)
```

---

## Dependency Graph

The strict dependency rule ensures that inner layers never reference outer layers:

```
Domain          ◄── depends on nothing
  ▲
  │
Application     ◄── depends on Domain
  ▲
  │
Infrastructure  ◄── depends on Application + Domain
```

At startup (`Main.kt`), dependencies are wired together through **constructor injection**:

```kotlin
val adapter   = ComposeAdapter(initialWorld)      // implements KeyboardInputPort + RenderPort
val engine    = GameEngine(initialWorld)           // holds domain state
val time      = TimeManager()                      // timing
val gameLoop  = GameLoop(engine, adapter, adapter, time)  // injected ports
```

`GameLoop` receives its ports as constructor parameters. It never knows -- or needs to know -- that `ComposeAdapter` is behind those interfaces. Swapping to a different renderer or input source is a one-line change at the composition root.

---

## Benefits in Practice

| Concern               | How the architecture helps                                                         |
|-----------------------|-----------------------------------------------------------------------------------|
| **Testing**           | Domain systems are pure functions. Pass in a `World`, assert on the returned `World`. No mocking needed. |
| **Determinism**       | Immutable state + fixed timestep = identical results given identical inputs. Essential for rollback netcode. |
| **Swappable UI**      | Replace `ComposeAdapter` with an OpenGL adapter -- `GameLoop` doesn't change.      |
| **Swappable network** | Replace `NetworkAdapter` with a local-loopback adapter for testing -- domain doesn't change. |
| **Onboarding**        | Each layer can be understood in isolation. Domain logic reads like game design docs. |

---

## Directory Reference

```
game/src/main/kotlin/com/heyteam/ufg/
├── domain/
│   ├── component/      # Value objects: Position, Health, InputState, Attack, ...
│   ├── entity/         # Aggregates: World, Player, Character
│   ├── system/         # Pure functions: GameLogic, PhysicsSystem, InputSystem, HitDetectionSystem
│   └── config/         # GameConstants
├── application/
│   ├── port/
│   │   ├── input/      # Driving ports: KeyboardInputPort, NetworkInputPort
│   │   └── output/     # Driven ports: RenderPort, NetworkOutputPort
│   └── service/        # GameLoop, GameEngine, TimeManager
├── infrastructure/
│   └── adapter/
│       ├── gui/        # ComposeAdapter, GameScreen, StageCanvas, Hud
│       └── network/    # NetworkAdapter, WebRtcBridge
└── Main.kt             # Composition root
```
