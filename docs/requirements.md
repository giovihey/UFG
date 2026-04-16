# Requirements

## Glossary

| Term | Definition |
|------|-----------|
| **Frame** | One tick of the game loop — 1/60th of a second (~16.67 ms) |
| **Input State** | Bitmask encoding which buttons are pressed in a given frame |
| **Hitbox** | Rectangle attached to an attack — overlapping an opponent's hurtbox registers a hit |
| **Hurtbox** | Rectangle representing a player's vulnerable area |
| **Frame data** | Startup, active, and recovery frame counts that define an attack's timing |
| **Hitstun** | Frames during which a hit player cannot act |
| **Knockback** | Horizontal push applied to a player when hit |
| **AABB** | Axis-Aligned Bounding Box — collision detection using non-rotated rectangles |
| **Lockstep** | Networking model where both peers must exchange input before advancing a frame |
| **SDP** | Session Description Protocol — text blob exchanged during WebRTC handshake |
| **ICE** | Interactive Connectivity Establishment — protocol for NAT traversal |

## Functional Requirements

### FR1: Real-Time Combat
Both players can move, jump, and attack. Inputs are processed every frame at 60 FPS. Attacks have frame data (startup/active/recovery) and hitboxes.

- Acceptance: pressing PUNCH triggers a JAB with 4 startup, 3 active, 8 recovery frames.

### FR2: Hit Detection and Damage
When an active hitbox overlaps an opponent's hurtbox, the opponent takes damage. Each attack can only hit once per activation.

- Acceptance: a JAB landing reduces opponent HP by the attack's damage value.

### FR3: Hitstun and Knockback
A hit puts the opponent in HITSTUN (unable to act) and pushes them away from the attacker.

- Acceptance: after being hit, the opponent cannot move or attack for the attack's `hitstunFrames` and slides in the knockback direction.

### FR4: Round Timer
A countdown timer starts at 99 seconds. When it reaches 0, the round ends. The player with more HP wins.

- Acceptance: timer decrements by 1 each second and triggers ROUND_END at 0.

### FR5: P2P Input Sharing
Both players' inputs are exchanged every frame over a WebRTC data channel. The game advances only when both inputs are available (lockstep).

- Acceptance: two players on different machines see identical game states frame-by-frame.

### FR6: NAT Traversal
Players behind home routers can connect without manual port forwarding, using STUN for NAT hole-punching.

- Acceptance: two players behind different NATs connect and play via the signaling server.

### FR7: Visual Feedback
The game renders players, hitboxes (during active frames), health bars, and the round timer. The viewport scales to fit the window.

- Acceptance: resizing the window scales the game area proportionally.

## Non-Functional Requirements

### NFR1: Determinism
Given identical inputs, the simulation must produce identical `World` states on both machines. Required for lockstep networking and future rollback netcode.

- Acceptance: all domain logic is pure (no randomness, no system clock, no mutable shared state).

### NFR2: 60 FPS Target
The game loop maintains a fixed timestep of 1/60s. Physics and input processing are decoupled from render rate.

- Acceptance: `TimeManager` accumulates time and steps at a fixed 60 Hz regardless of actual frame rate.

### NFR3: Low Latency Networking
Input packets use UDP-like delivery (unordered, no retransmission) over WebRTC data channels to minimize latency.

- Acceptance: data channel configured with unordered delivery and no retransmission.

### NFR4: Testability
All game logic can be tested without a GUI, network, or OS-level dependencies.

- Acceptance: domain systems are pure functions tested via Kotest specs.

## Distributed System Features

| Feature | Relevant | Rationale |
|---------|----------|-----------|
| **Transparency** | Partial | The P2P connection is hidden from gameplay, but connection setup (host/join) is visible to the user |
| **Fault tolerance** | Limited | If a peer disconnects, the game stalls (lockstep). Graceful shutdown is implemented but no reconnection |
| **Scalability** | No | 1v1 only. No matchmaking server, no spectators. P2P doesn't scale beyond 2 peers for this use case |
| **Security** | Partial | WebRTC encrypts the data channel (DTLS). No authentication — anyone with the signaling server URL can connect |
| **Performance** | Critical | Sub-frame latency required. P2P eliminates server hops. Fixed timestep ensures consistent simulation speed |
| **Consistency** | Critical | Both peers must agree on game state every frame. Achieved through deterministic lockstep simulation |

## Implementation Constraints

| Constraint | Justification |
|-----------|---------------|
| **Kotlin/JVM** | University course requirement. JVM enables cross-platform desktop deployment |
| **Compose Desktop** | Modern declarative UI framework for Kotlin, avoids Swing boilerplate |
| **libdatachannel (C++)** | Lightweight WebRTC library (~2 MB vs ~80 MB for full libwebrtc). Only data channels needed, not audio/video |
| **JNI bridge** | Required to call C++ WebRTC library from Kotlin/JVM |
| **Hexagonal Architecture** | Enforces determinism and testability by isolating domain from infrastructure |
