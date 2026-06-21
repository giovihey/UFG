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

## Matchmaking

Players do not pick a host or a room. Each client opens a WebSocket to the signaling
server and is placed in a **matchmaking queue**. The server pairs players on arrival:

- The first client to connect waits in the queue.
- The next client to connect is paired with the waiter. The server opens a fresh **room**,
  assigns the earlier player the `offerer` role and the new arrival the `answerer` role,
  and sends each a `{"type":"matched","role":...}` message.
- Only the offerer creates the WebRTC offer; from there the SDP/ICE/ready/start traffic is
  relayed **only between the two peers of that room**.

Because a new room is opened for every pair, the server supports an unbounded number of
concurrent matches — it is no longer limited to two players total. If a player disconnects
while queued, the queue is cleared; if a player disconnects mid-room, the room is dissolved
and the surviving peer receives `{"type":"peer_left"}`.

## Connection Flow

```
Player A                   Signaling Server                Player B
   │                              │                            │
   ├── WS connect ───────────────►│ (queued)                   │
   │                              │◄──────────── WS connect ───┤
   │                              │ pair → open room, assign   │
   │◄── "matched" {offerer} ──────┤── "matched" {answerer} ───►│
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
   │                              │                            │
   │── "ready" ──────────────────►├── forward ────────────────►│
   │◄──────────────── forward ────┤◄───────── "ready" ────────┤
   │                              │                            │
   │ (host) at = now + 500 ms     │                            │
   │── "start" {at} ─────────────►├── forward ────────────────►│
   │                              │                            │
   │◄──── both peers sleep until wall-clock == at ────────────►│
   │                              │                            │
   │── InputState (every frame) ──────────────────────────────►│
   │◄─────────────────────────────────── InputState (every frame)│
```

### Start-frame handshake

P2P means each peer's data channel reaches the "open" state on its own schedule —
DTLS finishes slightly later on one side than the other. If both peers immediately
started simulating at `frameNumber = 0`, the lagging peer could be 10+ frames behind
before it began, and rollback would clamp at `maxRollbackFrames = 8`, producing
permanent desync.

To avoid that, once the data channel is open the peers run a three-step protocol over
the signaling WebSocket (which stays connected after ICE/DTLS is done):

1. Each peer sends `{"type":"ready"}` and waits for its peer's ready.
2. Once both readys have crossed, the **host** picks `at = System.currentTimeMillis()
   + START_BUFFER_MS` (default 500 ms, comfortably greater than signaling RTT + JVM
   jitter) and sends `{"type":"start","at":<ms>}`.
3. Both peers (host and guest) sleep until wall-clock `at`, then simultaneously
   construct `TimeManager`, `GameEngine`, `RollbackService`, and start the loop.

`TimeManager`'s `lastFrameTime` is captured *at* that moment, not during the connect
wait, so its accumulator can't spike on first tick.

If the peer does not respond within `HANDSHAKE_TIMEOUT_MS` (5 s), the initiating side
surfaces "Peer did not respond to start handshake." on the UI and closes the
connection — no half-started simulation.

Beyond matchmaking (pairing players into rooms and assigning roles), the signaling server
is a pure message passthrough: it relays `ready`/`start` between the two peers of a room
without interpreting them.

## Architecture Integration

The network layer fits into hexagonal architecture as an infrastructure adapter:

```
application/
  port/input/NetworkInputPort.kt    # drainRemoteInputs() → List<FramedInput>, peerFrame() → Long
  port/input/FramedInput.kt         # (frame, InputState) DTO
  port/output/NetworkOutputPort.kt  # sendInput(state, frame, senderCurrentFrame) +
                                    # sendInputWindow(senderCurrentFrame, window)
  service/RollbackService.kt        # predict / advance / rewind / time-sync stall

infrastructure/adapter/network/
  NetworkAdapter.kt                 # Implements both ports, ConcurrentLinkedQueue + map +
                                    # AtomicLong tracking peer's latest sim frame
  WebRtcBridge.kt                   # JNI bridge to C++ libdatachannel
  SignalingClient.kt                # WebSocket client for SDP/ICE exchange
```

### Threading Model

Two threads share data via lock-free collections:

- **JNI thread** (libdatachannel): receives bytes from remote, calls `onRemoteInput()` → enqueues into `ConcurrentLinkedQueue<FramedInput>` (and also updates a legacy `ConcurrentHashMap<Long, InputState>` for any remaining `pollRemoteInput` caller).
- **Game loop thread**: calls `drainRemoteInputs()` once per tick → empties the queue.

**The game loop no longer blocks.** When the remote input for the current frame has not yet arrived, `RollbackService` predicts it (repeat last known) and advances; a later authoritative packet triggers rewind-and-replay if the prediction was wrong. See [Rollback Netcode](rollback.md).

## C++ / JNI Layer

`WebRtcBridge.kt` loads `libwebrtc_wrapper` via `System.loadLibrary()` and declares `external` functions for each native operation. JNI callbacks (`onLocalDescription`, `onLocalCandidate`, `onRemoteInput`, `onDataChannelOpen/Close`) delegate to Kotlin listener interfaces.

### Wire format for input packets

Each gameplay packet is **36 bytes**, packed in `webrtc_wrapper.cpp` and unpacked in the matching `onMessage` handler:

| Offset | Size | Field | Meaning |
|---|---|---|---|
| 0 | 4 | `inputMask` (`int32`) | Bitmask of buttons held this frame. |
| 4 | 8 | `frameNumber` (`int64`) | The frame this input belongs to (sender's scheduled frame after `inputDelay`). |
| 12 | 8 | `senderCurrentFrame` (`int64`) | The frame the sender is currently simulating at the moment of the send. Receiver uses this to detect when its peer is ahead and stall a tick. |
| 20 | 8 | `committedFrame` (`int64`) | The frame `committedHash` is over, or `LLONG_MIN` if the sender has no committed hash yet. |
| 28 | 8 | `committedHash` (`int64`) | Canonical [`WorldHash`](rollback.md#canonical-hash) of the sender's `committedFrame`. Receiver compares against its own hash for the same frame; mismatch fires an `onDesync` event. Ignored when `committedFrame == LLONG_MIN`. |

Both peers must run a `webrtc_wrapper.dylib` built from the matching header — a sender packing fewer bytes against a receiver expecting 36 (or vice versa) will scramble frame numbers silently. Rebuild after any wire-format change with `cd channel && cmake --build build`.

The local test (`channel/test/local_test.cpp`) validates the C++ layer by simulating two peers in the same process — passing SDP and ICE candidates directly between objects without a signaling server.
