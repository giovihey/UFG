# Signaling server benchmark

Generated: 2026-06-23T00:36:11+02:00  
Target: `ws://localhost:8080/ws`  
Relay round-trips per room: 20

Each level spawns `2 x Rooms` WebSocket clients that the server pairs into rooms. **Match** columns are time from connect to the `matched` message; **Relay** columns are message round-trip latency through a room (offerer -> server -> answerer -> server -> offerer). **Match/s** is matched rooms per second.

| Rooms | Clients | Matched | Match/s | Match p50 | Match p95 | Match p99 | Relay p50 | Relay p95 | Relay p99 | Errors |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 2 | 2/2 | 366 | 2.466ms | 2.611ms | 2.611ms | 515µs | 775µs | 810µs | 0 |
| 10 | 20 | 20/20 | 781 | 10.522ms | 12.19ms | 12.382ms | 1.144ms | 2.564ms | 3.187ms | 0 |
| 50 | 100 | 100/100 | 1600 | 25.933ms | 29.754ms | 30.05ms | 2.02ms | 4.152ms | 6.481ms | 0 |
| 100 | 200 | 200/200 | 2625 | 28.532ms | 35.413ms | 35.802ms | 2.907ms | 4.974ms | 7.589ms | 0 |
| 250 | 500 | 500/500 | 2576 | 53.999ms | 91.384ms | 93.944ms | 7.195ms | 11.009ms | 12.861ms | 0 |
| 500 | 1000 | 1000/1000 | 1091 | 131.978ms | 169.911ms | 411.027ms | 14.442ms | 40.521ms | 56.814ms | 0 |

> Numbers are localhost measurements; they isolate server overhead and exclude real WAN/NAT latency.
