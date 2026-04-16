# Concept

## Project Type

**Desktop application** — a real-time 2D fighting game with a graphical interface, built with Kotlin and Compose Desktop. Two players connect peer-to-peer and fight in real-time.

## Use Case

### What
UFG is a 1v1 fighting game where two players on separate machines exchange inputs every frame (~16 ms) and run identical game simulations locally. Each player controls a fighter with movement, jumps, and attacks. Hits deal damage; the first player to reach 0 HP or the player with more HP when the timer expires wins the round.

### Where
Players can be anywhere with an internet connection. The game uses WebRTC with NAT traversal (STUN), so players behind home routers can connect directly without port forwarding.

### How
Each player runs the game on their desktop (macOS, Linux, or Windows). One player hosts (creates a WebRTC offer), the other joins. After a brief handshake through a signaling server, the connection is direct P2P — the signaling server is no longer needed.

### Devices
Desktop computers with a keyboard. The game window is 800x600 logical units, scaled to fit the display.

### Data Storage
No persistent storage. All game state lives in memory during a match and is discarded when the game closes.

### User Roles
Both players are equal peers. The only asymmetry is during connection setup: one player is the **host** (creates the WebRTC offer) and the other is the **joiner** (receives it). Once connected, both run identical logic.

## Why Distribution?

### Real-Time Input Sharing
A fighting game requires both players' inputs to compute each frame. Distribution is not optional — it's the core mechanic. Each frame, the local input is sent to the remote peer and the remote input is received, so both machines can simulate the same game state.

### Latency Sensitivity
Fighting games are measured in frames (1 frame = ~16 ms at 60 FPS). A 3-frame delay is already noticeable. This rules out client-server architectures where every input round-trips through a central server. **P2P** eliminates the extra hop — packets flow directly between players.

### No Central Server Dependency
After the initial signaling handshake, the game runs entirely between the two peers. There is no server to host, pay for, or scale. The signaling server is lightweight and stateless — it only relays SDP/ICE messages during connection setup.

### Determinism
Both machines must produce identical game states given the same inputs. This is a distribution constraint: the game logic must be pure and deterministic, with no reliance on local randomness or timing differences. The architecture enforces this through immutable state and a fixed timestep.
