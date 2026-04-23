# Logging

UFG uses **SLF4J** as the logging API, **Logback Classic** as the binding, and
**kotlin-logging** as the idiomatic Kotlin wrapper. Every `println` in production code
has been replaced with a structured logger call; stack traces go through `log.error(ex)` so
they remain correlated with the surrounding message and do not leak directly to `System.err`.

The configuration lives at `game/src/main/resources/logback.xml`.

## Why this stack

| Concern | Choice | Reason |
|---|---|---|
| API | `org.slf4j:slf4j-api` | Ubiquitous JVM logging façade; lets backends be swapped without touching code. |
| Binding | `ch.qos.logback:logback-classic` | First-class SLF4J backend with per-logger levels, XML/Groovy config, async appenders. Replaces the previous `slf4j-simple`, which has no config file. |
| Wrapper | `io.github.oshai:kotlin-logging-jvm` | Gives `private val log = KotlinLogging.logger {}` and lazy blocks `log.debug { "…" }` — zero string construction when the level is disabled, which matters in a 60 Hz loop. |

## Log levels

| Level | Use for |
|---|---|
| `error` | Unrecoverable, broken invariants, fatal exceptions. Always include `throwable`. |
| `warn`  | Something went wrong but the game continues, peer disconnected, WebSocket not open, rollback clamp fired. |
| `info`  | Lifecycle transitions visible to an operator, signaling connected, peer connected, match over. |
| `debug` | Per-frame / per-packet chatter — JNI callbacks, signaling messages. Off by default. |

Per-frame chatter **must** be at `debug`. Anything emitted once per tick at `info` will flood
the terminal at 60 Hz and drown the netcode stream.

## Netcode observability

`RollbackService` itself is a pure state machine: it contains no logging or metrics code.
Observability is wired through a `RollbackListener` interface:

```kotlin
interface RollbackListener {
    fun onPredictionEvaluated(hit: Boolean) {}
    fun onRollback(event: RollbackEvent) {}
    fun onFrameAdvanced(currentFrame: Long) {}
}
```

`GameLoop` instantiates two listeners and composes them:

```kotlin
listener = CompositeRollbackListener(
    listOf(NetcodeEventLogger(), NetcodeStatsLogger()),
)
```

- **`NetcodeEventLogger`** — emits `[RB]` / `[RB-CLAMP]` lines on every rollback.
- **`NetcodeStatsLogger`** — accumulates rollbacks, prediction hits, and auth-lag, and
  emits one `[NETCODE]` summary line per wall-clock second on `onFrameAdvanced`.

To silence the stream, swap the composite for `NoopRollbackListener`. To drive a replay
viewer, add a third listener that forwards events into a queue. The rollback service
itself does not need to change.

The listeners publish to a dedicated logger named `netcode`. That logger is bound
in `logback.xml` to a separate appender with pattern `%msg%n` — no timestamp, no level, no
logger name prefix — so the lines are greppable, parseable, and ready to pipe to `awk` /
`jq` / a plotting script.

### `[RB]` — one line per rollback

Emitted **only** when a newly-received authoritative remote input disagrees with what was
predicted for a past frame, forcing a rewind-and-replay.

```
[RB] t=12.43s frame=1234 rewind=4f misprediction_frame=1230 remote_predicted=0x42 remote_actual=0x02 auth_lag=3f
```

| Field | Meaning |
|---|---|
| `t`                   | Wall-clock seconds since the rollback service was constructed. |
| `frame`               | Current local simulation frame when the rewind was triggered. |
| `rewind`              | How many frames were actually replayed (`frame` − rewound-to frame). |
| `misprediction_frame` | The earliest frame whose predicted remote input turned out wrong. |
| `remote_predicted`    | The input mask we had assumed for the peer at that frame (`0xHH`). |
| `remote_actual`       | The authoritative input the peer had actually sent. |
| `auth_lag`            | How many frames behind the local frame the authoritative packet arrived. |

**Silence means a clean run.** No rollbacks fired → no `[RB]` lines. A reviewer reading the
terminal can see, frame-accurately, exactly when and why each rewind happened.

If the service had to clamp the rewind to `maxRollbackFrames`, an additional
`[RB-CLAMP]` warning is emitted — that indicates a genuine divergence where a visual pop
is possible.

### `[NETCODE]` — one line per second

The ambient pulse. Emitted once per wall-clock second as long as the simulation is
advancing.

```
[NETCODE] f=1234 rb/s=2.1 predicted=73% auth_lag_avg=2.8f auth_lag_max=5f
```

| Field | Meaning |
|---|---|
| `f`              | Local simulation frame at the moment of the flush. |
| `rb/s`           | Rollbacks per second observed during the window. |
| `predicted`      | Percentage of prediction evaluations that matched the authoritative input. |
| `auth_lag_avg`   | Mean `auth_lag` of the rollbacks in the window (frames). |
| `auth_lag_max`   | Worst-case `auth_lag` of the window (frames). |

Toggle simulated latency and this line visibly changes — the intended "visual feedback
without any GUI work".

## `--fake-lag=N` demo flag

On loopback, authoritative remote inputs always arrive before they are needed, so no
rewind ever fires and `rb/s` stays at `0.0`. To demonstrate rollback behaviour, start the
client with:

```
./gradlew run --args="--host --fake-lag=4"
```

The flag wires an extra `FakeLagInputPort` decorator around the real network port. It
holds each drained remote input for `N` local ticks before releasing it to the rollback
service. Typical demo matrix:

| `--fake-lag` | Expected behaviour |
|---|---|
| `0` (default) | `rb/s=0.0`. No rollbacks. |
| `2`           | Occasional rollbacks when inputs change within the delay window. |
| `4`           | Frequent rollbacks, `auth_lag_avg ≈ 3f`. |
| `8`           | Rollbacks pinned at `maxRollbackFrames`, expect `[RB-CLAMP]` warnings. |

Run two clients side-by-side with different values and watch the summary lines diverge —
this is the demonstration the system was instrumented for.

## Changing log levels

Edit `game/src/main/resources/logback.xml`:

```xml
<!-- Silence netcode entirely -->
<logger name="netcode" level="OFF"/>

<!-- Show WebRTC/JNI chatter -->
<logger name="com.heyteam.ufg.infrastructure.adapter.network" level="DEBUG"/>
```

To override at runtime without rebuilding, pass a different config file:

```
./gradlew run --args="--host" -Dlogback.configurationFile=/absolute/path/to/logback.xml
```

## Adding a logger

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class MyThing {
    fun doWork() {
        log.debug { "expensive details: ${computeExpensive()}" }
        log.info  { "started" }
    }
}
```

Always prefer the `{ "…" }` lazy form. It costs nothing when the level is disabled.
