# Networking — P2P Input Sharing

This document explains how the peer-to-peer networking layer works in UFG, covering WebRTC, the signaling process, and the C++ data channel implementation.

## The Problem

Two players need to exchange input every frame with minimal latency. Traditional client-server adds a round trip through a central server. Instead, we use **peer-to-peer**: packets flow directly between the two players' machines.

## Why WebRTC

WebRTC (Web Real-Time Communication) provides three things we need:

1. **UDP-based data channels** — Unlike TCP, UDP doesn't wait for lost packets to be retransmitted. In a fighting game, a 1-frame-old input is useless — we'd rather skip it than wait.
2. **NAT traversal** — Most computers sit behind a home router (NAT) and aren't directly reachable. WebRTC handles punching through NATs automatically.
3. **Direct P2P connection** — After an initial handshake, data flows directly between players with no server in the middle.

We use **libdatachannel** (a lightweight C++ WebRTC library) because we only need data channels, not audio or video. It's ~2 MB vs ~80 MB for Google's full libwebrtc.

## Key Concepts

### SDP (Session Description Protocol)

Before two peers can connect, they need to agree on *how* to communicate — codecs, encryption, network info. This negotiation happens via SDP messages:

- **Offer**: Peer A says "here's what I support and how to reach me"
- **Answer**: Peer B says "I accept, here's my info in return"

These are just text blobs exchanged through the signaling server. Neither peer interprets the other's SDP — they hand it to their WebRTC library, which handles it.

### ICE (Interactive Connectivity Establishment)

ICE solves the problem: **how do two computers behind routers find a network path to each other?**

Most computers have a private IP (like `192.168.1.5`) that only exists inside their local network. You can't send a packet to someone else's `192.168.1.x` — that address means nothing outside their home.

ICE gathers **candidates** (possible network paths) and tries them in order:

#### Candidate 1 — Host (direct LAN)

```
Player A ──────────── Player B
       same WiFi network
```

Both players are on the same local network. Use private IPs directly. Fastest possible path, near-zero latency.

#### Candidate 2 — Server Reflexive (via STUN)

```
Player A ──► STUN server says "your public IP is 85.23.1.100:43210"
Player B ──► STUN server says "your public IP is 91.45.2.200:55321"

Player A ── 85.23.1.100:43210 ◄────────► 91.45.2.200:55321 ── Player B
                        NAT "hole" punched
```

A **STUN server** is a simple public server that tells you what your IP and port look like from the outside. It's like asking someone on the street "what's my address?" — you can't see your public IP yourself because you're behind a router.

Once both sides know their public IP:port, they send UDP packets directly to each other. Most NATs allow this ("hole punching"). Works ~80% of the time.

We use Google's free STUN server: `stun:stun.l.google.com:19302`

#### Candidate 3 — Relay (via TURN, last resort)

```
Player A ──► TURN server ◄── Player B
              (relay)
```

Some strict NATs block hole punching entirely. A **TURN server** relays all traffic between the two players. This always works but adds latency and costs money (the server handles all game traffic). We don't use TURN currently.

#### How ICE works in practice

```
1. Both peers create a PeerConnection
2. Each peer gathers candidates:
   - "I'm at 192.168.1.5:12345"           (host candidate)
   - "I'm at 85.23.1.100:43210"           (STUN/server-reflexive candidate)
3. They exchange candidates via the signaling server
4. ICE tries each pair (A's candidate × B's candidate)
5. First working pair wins — that becomes the connection path
```

The `onLocalCandidate` callback fires once per candidate found. In the code, we forward each candidate to the other peer immediately.

### Data Channels

Once ICE establishes a path, WebRTC opens a **data channel** — a bidirectional pipe for arbitrary data. We configure ours for:

- **Unordered delivery** — packets arrive in whatever order, no head-of-line blocking
- **No retransmission** — lost packets are gone (we'd rather use the next frame's input)

This gives us UDP-like behavior wrapped in WebRTC's encryption and NAT traversal.

## Connection Flow

```
Player A                   Signaling Server (Go)              Player B
   │                              │                               │
   ├── create PeerConnection      │                               │
   ├── create DataChannel         │                               │
   │                              │                               │
   │   (WebRTC generates SDP      │                               │
   │    offer automatically)      │                               │
   │                              │                               │
   ├── SDP offer ────────────────►├── forward ───────────────────►│
   │                              │                  create PeerConnection
   │                              │                  setRemoteDescription
   │                              │                               │
   │                              │   (WebRTC generates SDP       │
   │                              │    answer automatically)      │
   │                              │                               │
   │◄──────────────── forward ────┤◄───────────── SDP answer ────┤
   │                              │                               │
   │  setRemoteDescription        │                               │
   │                              │                               │
   ├── ICE candidates ──────────►├── forward ───────────────────►│
   │◄──────────────── forward ────┤◄────────── ICE candidates ───┤
   │                              │                               │
   │◄═══════════ P2P connection established ═════════════════════►│
   │          (signaling server no longer needed)                  │
   │                              │                               │
   │── InputState (every frame) ─────────────────────────────────►│
   │◄────────────────────────────────────── InputState (every frame)│
```

## The Local Test (`channel/test/local_test.cpp`)

The test validates that libdatachannel works by simulating two peers **in the same process**. No signaling server is needed — we pass SDP and ICE candidates directly between objects.

### What the test does, step by step

```cpp
// Empty config — no STUN needed since both peers are on localhost
rtc::Configuration config;

auto peerA = make_shared<rtc::PeerConnection>(config);
auto peerB = make_shared<rtc::PeerConnection>(config);
```

Two PeerConnection objects in the same process. In production, these would be on different machines.

```cpp
peerA->onLocalDescription([&peerB](rtc::Description desc) {
    peerB->setRemoteDescription(desc);
});
```

When Peer A generates its SDP offer, we hand it directly to Peer B. In production, this would go through the Go signaling server via WebSocket.

```cpp
peerA->onLocalCandidate([&peerB](rtc::Candidate candidate) {
    peerB->addRemoteCandidate(candidate);
});
```

Same for ICE candidates — forwarded directly instead of through a server. This callback fires multiple times as different candidates are discovered.

```cpp
peerB->onDataChannel([&dataChannelOpen](shared_ptr<rtc::DataChannel> dc) {
    // B receives the channel that A created
    dc->onMessage([](auto message) {
        // Called when data arrives from A
    });
    dc->onOpen([dc, &dataChannelOpen]() {
        dc->send("hello from B");
        dataChannelOpen = true;
    });
});
```

Peer B didn't create a data channel — Peer A did. When B's PeerConnection receives it, `onDataChannel` fires. Only the **initiating** side calls `createDataChannel()`; the other side receives it via this callback.

```cpp
auto dc = peerA->createDataChannel("input");
```

This triggers the entire connection process:
1. A creates the data channel named "input"
2. WebRTC generates an SDP offer (fires `onLocalDescription`)
3. The offer reaches B (we forward it manually)
4. B generates an SDP answer
5. ICE candidates are exchanged
6. Connection opens, both sides can send/receive

### Expected output

```
Waiting for connection...
[A] Generated local description, passing to B
[B] Generated local description, passing to A
[B] Received data channel: input
[A] Data channel open, sending message
[B] Data channel open, sending reply
[B] Received: hello from A
[A] Received: hello from B
TEST PASSED
```

### What this proves

- libdatachannel is correctly installed and linked
- PeerConnection creation and SDP exchange work
- ICE candidate exchange and connection establishment work
- Bidirectional message passing over data channels works
- The foundation is solid for building the JNI wrapper on top

## C++ Concepts Used

| Concept | Explanation |
|---|---|
| `shared_ptr<T>` | Smart pointer that automatically frees memory when nothing references it. C++ has no garbage collector — without smart pointers, you must manually `delete` objects or risk memory leaks. |
| `make_shared<T>(args)` | Creates an object wrapped in a shared_ptr. Preferred over `new` + raw pointer. |
| `atomic<bool>` | Thread-safe boolean. libdatachannel callbacks run on background threads — a normal `bool` read/written from multiple threads is a data race (undefined behavior). |
| `[&peerB](args) { ... }` | Lambda capturing `peerB` by reference (`&`). Lambdas are anonymous functions; the capture list (`[]`) specifies which outside variables the lambda can access. |
| `[dc, &flag]()` | Mixed capture: `dc` by value (copies the shared_ptr, incrementing its reference count) and `flag` by reference. |
| `holds_alternative<string>(msg)` | Checks which type a `std::variant` holds. libdatachannel messages are `variant<string, binary>` — could be text or raw bytes. |
| `get<string>(msg)` | Extracts the string from the variant after checking its type. |
| `this_thread::sleep_for(...)` | Blocks the current thread. Needed because WebRTC operations are asynchronous — callbacks fire on internal threads. |

## Architecture Integration

The C++ data channel fits into the game's hexagonal architecture as an infrastructure adapter:

```
domain/
  component/InputState.kt          # Already exists — bitmask of pressed buttons

application/
  port/input/NetworkInputPort.kt   # Interface: pollRemoteInput(frame) → InputState?
  port/output/NetworkOutputPort.kt # Interface: sendInput(state, frame)

infrastructure/
  adapter/network/
    WebRtcBridge.kt                # JNI bridge — declares `external` functions
    NetworkAdapter.kt              # Implements both ports using WebRtcBridge
  native/
    webrtc_wrapper.cpp             # C++ JNI implementation using libdatachannel
```

The game loop sends local input through `NetworkOutputPort` and reads remote input from `NetworkInputPort`. The adapter translates between Kotlin `InputState` (an `Int` bitmask) and raw bytes sent over the WebRTC data channel.
