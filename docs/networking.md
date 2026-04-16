# Networking — P2P Input Sharing

## The Problem

Two players need to exchange input every frame (~16 ms) with minimal latency. Client-server adds a round trip through a central server. **P2P** eliminates that hop — packets flow directly between machines.

## Why WebRTC

WebRTC provides three things a fighting game needs:

1. **UDP-based data channels** — no head-of-line blocking. A 1-frame-old input is useless; we'd rather skip it than wait.
2. **NAT traversal** — most computers sit behind a router. WebRTC handles hole-punching automatically.
3. **Direct P2P** — after the handshake, data flows directly between players. No server in the middle.

We use **libdatachannel** (a lightweight C++ WebRTC library, ~2 MB) because we only need data channels, not audio/video. Google's full libwebrtc is ~80 MB.

## Key Concepts

### SDP (Session Description Protocol)

Before connecting, peers exchange SDP messages to agree on how to communicate:

- **Offer**: Peer A says "here's what I support and how to reach me"
- **Answer**: Peer B replies with its own info

These are text blobs relayed through the signaling server. Neither peer interprets the other's SDP — they hand it to their WebRTC library.

### ICE (Interactive Connectivity Establishment)

ICE solves: **how do two computers behind routers find a network path?**

ICE gathers **candidates** (possible paths) and tries them in order:

| Candidate | How it works | When it's used |
|-----------|-------------|----------------|
| **Host** | Direct LAN connection using private IPs | Both players on same network. Near-zero latency |
| **Server Reflexive (STUN)** | A public STUN server tells each peer its external IP:port. Peers send UDP directly to each other's public address ("hole punching") | ~80% of internet connections. We use `stun:stun.l.google.com:19302` |
| **Relay (TURN)** | A TURN server relays all traffic | Strict NATs that block hole-punching. Adds latency, costs money. Not used currently |

ICE tries each pair (A's candidate x B's candidate) and uses the first one that works.

### Data Channels

Once ICE finds a path, WebRTC opens a **data channel** — a bidirectional pipe for arbitrary data. Ours is configured for:

- **Unordered delivery** — no head-of-line blocking
- **No retransmission** — lost packets are gone (we'd rather use the next frame's input)

This gives UDP-like behavior wrapped in WebRTC's DTLS encryption and NAT traversal.

## Connection Flow

```
Player A                   Signaling Server                Player B
   │                              │                            │
   ├── create PeerConnection      │                            │
   ├── create DataChannel("input")│                            │
   │                              │                            │
   ├── SDP offer ────────────────►├── forward ────────────────►│
   │                              │               setRemoteDescription
   │                              │                            │
   │◄──────────────── forward ────┤◄───────── SDP answer ─────┤
   │  setRemoteDescription        │                            │
   │                              │                            │
   ├── ICE candidates ──────────►├── forward ────────────────►│
   │◄──────────────── forward ────┤◄──── ICE candidates ──────┤
   │                              │                            │
   │◄══════════ P2P connection established ═══════════════════►│
   │        (signaling server no longer needed)                │
   │                              │                            │
   │── InputState (every frame) ──────────────────────────────►│
   │◄─────────────────────────────────── InputState (every frame)│
```

## Architecture Integration

The network layer fits into hexagonal architecture as an infrastructure adapter:

```
application/
  port/input/NetworkInputPort.kt    # pollRemoteInput(frame) → InputState?
  port/output/NetworkOutputPort.kt  # sendInput(state, frame)

infrastructure/adapter/network/
  NetworkAdapter.kt                 # Implements both ports, uses ConcurrentHashMap
  WebRtcBridge.kt                   # JNI bridge to C++ libdatachannel
  SignalingClient.kt                # WebSocket client for SDP/ICE exchange
```

### Threading Model

Two threads share data via `ConcurrentHashMap<Long, InputState>`:

- **JNI thread** (libdatachannel): receives bytes from remote, calls `onRemoteInput()` → puts in map
- **Game loop thread**: calls `pollRemoteInput(frame)` → removes from map

The game loop blocks on `pollRemoteInput()` until the remote input arrives — this is lockstep synchronization.

## C++ / JNI Layer

`WebRtcBridge.kt` loads `libwebrtc_wrapper` via `System.loadLibrary()` and declares `external` functions for each native operation. JNI callbacks (`onLocalDescription`, `onLocalCandidate`, `onRemoteInput`, `onDataChannelOpen/Close`) delegate to Kotlin listener interfaces.

The local test (`channel/test/local_test.cpp`) validates the C++ layer by simulating two peers in the same process — passing SDP and ICE candidates directly between objects without a signaling server.
