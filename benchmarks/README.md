# Benchmarks

Load harness for UFG's distributed components. Currently covers the **signaling server**.

## Signaling server (`signaling/`)

A self-contained Go program that speaks the signaling server's matchmaking protocol and
measures what the server is actually responsible for:

- **Time-to-match** — latency from a client connecting to receiving its `matched` message,
  under a burst of `2N` simultaneous joins.
- **Relay RTT** — round-trip latency of a message relayed through a room
  (`offerer → server → answerer → server → offerer`). This is the latency the SDP/ICE
  handshake experiences.

It sweeps an increasing number of concurrent rooms so you can see how both metrics hold up
under load. Each room is two clients: the server assigns one `offerer` (which drives the
ping round-trips) and one `answerer` (which echoes them back).

### Run

1. Start the signaling server:
   - `make signaling` (Docker — exposes `:8080`), **or**
   - `cd signaling_server && go run .` (docker-free local measurement).
2. Run the benchmark:
   ```
   make bench-signaling
   # or directly, with options:
   cd benchmarks/signaling && go run . -sweep 1,10,50,100 -pings 20
   ```

Pass extra flags through the Makefile with `BENCH_ARGS`:
```
make bench-signaling BENCH_ARGS="-sweep 1,5 -pings 5"
```

### Flags

| Flag       | Default                  | Meaning                                      |
|------------|--------------------------|----------------------------------------------|
| `-url`     | `ws://localhost:8080/ws` | signaling server WebSocket URL               |
| `-sweep`   | `1,10,50,100,250,500`    | comma-separated room counts to test          |
| `-rooms`   | `0`                      | single room count (overrides `-sweep`)       |
| `-pings`   | `20`                     | relay round-trips measured per room          |
| `-out`     | `../RESULTS.md`          | markdown results file (empty string to skip) |
| `-timeout` | `30s`                    | per-level safety timeout                     |

### Reading the output

Results print as an aligned table and are written to [`RESULTS.md`](RESULTS.md). `Match/s`
is matched rooms per second; `Match p*` / `Relay p*` are latency percentiles. `Errors` counts
failed dials plus protocol errors.

### Notes

- Numbers are **localhost** measurements: they isolate server overhead and exclude real
  WAN/NAT latency.
- The top sweep level (`500` rooms) opens `1000` sockets at once. macOS defaults to a low
  file-descriptor limit — if you see dial errors at high levels, raise it for the shell with
  `ulimit -n 4096` before running.
