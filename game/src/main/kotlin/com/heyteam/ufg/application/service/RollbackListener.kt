package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.component.InputState

/**
 * Details of a single rewind-and-replay. Emitted by [RollbackService] exactly once per
 * rollback, at the moment the replay has completed.
 *
 * @property currentFrame        the live simulation frame when the rewind fired.
 * @property mispredictionFrame  the earliest frame whose predicted remote input was wrong.
 * @property effectiveFromFrame  the frame we actually restored from, after clamping by
 *                               [RollbackConfig.maxRollbackFrames]. Equal to
 *                               [mispredictionFrame] except when the clamp fires.
 * @property rewindFrames        how many frames were re-simulated (`currentFrame - effectiveFromFrame`).
 * @property authLag             `currentFrame - mispredictionFrame` — how far behind the
 *                               live frame the authoritative packet arrived.
 * @property predicted           the remote input mask we had assumed for [mispredictionFrame].
 * @property actual              the authoritative remote input for [mispredictionFrame].
 */
data class RollbackEvent(
    val currentFrame: Long,
    val mispredictionFrame: Long,
    val effectiveFromFrame: Long,
    val rewindFrames: Int,
    val authLag: Int,
    val predicted: InputState,
    val actual: InputState,
) {
    /** True when the rewind had to be capped by the [RollbackConfig.maxRollbackFrames] guard. */
    val clamped: Boolean get() = effectiveFromFrame > mispredictionFrame
}

/**
 * Fired when a peer-reported committed-frame hash disagrees with ours for the same frame.
 * The two simulations have diverged, which means either:
 *  - a determinism bug somewhere in `domain/` (most likely),
 *  - or a clamped correction snuck through despite time-sync stalling,
 *  - or game data on the two clients differs (e.g. JSON character files).
 *
 * The listener is the only signal — there is currently no automatic recovery; a real
 * shipping title would force a resync via authoritative state retransmission.
 */
data class DesyncEvent(
    val currentFrame: Long,
    val desyncFrame: Long,
    val localHash: Long,
    val peerHash: Long,
)

/**
 * Observer hook for [RollbackService]. Implementations can log, record metrics, drive a
 * replay viewer, etc. All methods default to no-ops so listeners only need to override the
 * events they care about. Listeners are invoked synchronously on the game-loop thread and
 * must not block.
 */
interface RollbackListener {
    /** Called every time an authoritative remote input was compared against a prediction. */
    fun onPredictionEvaluated(hit: Boolean) {}

    /** Called once per completed rewind-and-replay. */
    fun onRollback(event: RollbackEvent) {}

    /** Called at the end of every simulated frame; useful for periodic flushes. */
    fun onFrameAdvanced(currentFrame: Long) {}

    /**
     * Called when a tick was skipped because we are running ahead of the peer. [advantage]
     * is `currentFrame - peerFrame` at the moment of the stall. Stalling is how we keep the
     * gap from exceeding [RollbackConfig.maxRollbackFrames] and silently dropping
     * authoritative corrections.
     */
    fun onStall(
        currentFrame: Long,
        advantage: Int,
    ) {}

    /**
     * Called when our committed-frame hash for some past frame disagrees with the peer's
     * hash for the same frame. Indicates a confirmed cross-machine desync.
     */
    fun onDesync(event: DesyncEvent) {}
}

/** A [RollbackListener] that does nothing. Default for [RollbackService]. */
object NoopRollbackListener : RollbackListener

/** Fans a single event stream out to multiple listeners, in declaration order. */
class CompositeRollbackListener(
    private val listeners: List<RollbackListener>,
) : RollbackListener {
    override fun onPredictionEvaluated(hit: Boolean) {
        for (l in listeners) l.onPredictionEvaluated(hit)
    }

    override fun onRollback(event: RollbackEvent) {
        for (l in listeners) l.onRollback(event)
    }

    override fun onFrameAdvanced(currentFrame: Long) {
        for (l in listeners) l.onFrameAdvanced(currentFrame)
    }

    override fun onStall(
        currentFrame: Long,
        advantage: Int,
    ) {
        for (l in listeners) l.onStall(currentFrame, advantage)
    }

    override fun onDesync(event: DesyncEvent) {
        for (l in listeners) l.onDesync(event)
    }
}
