package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.application.port.input.NetworkInputPort
import com.heyteam.ufg.application.port.output.NetworkOutputPort
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.entity.World

/**
 * Rollback netcode orchestrator.
 *
 * Each tick the service:
 *  1. Drains any new authoritative remote inputs from the network (non-blocking).
 *  2. If a newly-received authoritative input disagrees with what we predicted for a past
 *     frame, restores the pre-frame snapshot and re-simulates forward to the present using
 *     stored local inputs (always authoritative) and the corrected remote inputs.
 *  3. Consumes the local input (delayed by [inputDelay] frames) and a predicted remote
 *     input (repeat-last) and advances the simulation one frame via [GameLogic.step].
 *  4. Snapshots the post-step [World] and sends a redundant window of recent local inputs
 *     to the peer.
 *
 * Because every domain type ([World], [Player][com.heyteam.ufg.domain.entity.Player],
 * [InputState], etc.) is an immutable data class, "snapshotting" is free: we just keep the
 * prior [World] reference. Restoring is equally free.
 *
 * Observability is handled by a pluggable [RollbackListener]. The default
 * [NoopRollbackListener] is silent; wire in [NetcodeEventLogger] / [NetcodeStatsLogger]
 * (or both via [CompositeRollbackListener]) to surface rewind events and per-second
 * summaries.
 *
 * @param localPlayerId  the player ID controlled by this client (1 or 2).
 * @param remotePlayerId the player ID controlled by the peer.
 * @param inputDelay     frames of local input delay (GGPO-style). Hides jitter up to this
 *                       window without any rollback at all. Typical: 2.
 * @param maxRollbackFrames hard cap on how far back we will restore + re-simulate.
 *                          Typical: 8 (≈133 ms at 60 Hz).
 * @param sendWindow     how many recent local frames are included in every outbound packet
 *                       to mask single-packet loss. Typical: 8 (same as maxRollbackFrames).
 */
data class RollbackConfig(
    val inputDelay: Int = RollbackService.DEFAULT_INPUT_DELAY,
    val maxRollbackFrames: Int = RollbackService.DEFAULT_MAX_ROLLBACK_FRAMES,
    val sendWindow: Int = RollbackService.DEFAULT_SEND_WINDOW,
)

@Suppress("LongParameterList")
class RollbackService(
    private val engine: GameEngine,
    private val networkInput: NetworkInputPort,
    private val networkOutput: NetworkOutputPort,
    private val localPlayerId: Int,
    private val remotePlayerId: Int,
    config: RollbackConfig = RollbackConfig(),
    private val listener: RollbackListener = NoopRollbackListener,
) {
    private val inputDelay: Int = config.inputDelay
    private val maxRollbackFrames: Int = config.maxRollbackFrames
    private val sendWindow: Int = config.sendWindow

    // Frame-keyed input tables. Always authoritative on the local side; on the remote side
    // an entry exists only once the peer's packet has been received.
    private val localInputs = HashMap<Long, InputState>()
    private val remoteAuthoritative = HashMap<Long, InputState>()

    // Snapshot ring: worldBeforeFrame[N] = the World that WILL BE the input to step() for
    // frame N. (Equivalently: the post-step World of frame N-1.) On rollback we look up
    // worldBeforeFrame[K] and restore it, then replay frames K..currentFrame-1.
    private val worldBeforeFrame = HashMap<Long, World>()

    // Predicted-remote-input tracking: to know whether a late authoritative input forces a
    // rollback, we remember what we assumed at each frame.
    private val predictedRemote = HashMap<Long, InputState>()

    // Highest frame for which we have the remote's authoritative input. Used to decide when
    // it is safe to forget older snapshots/inputs.
    private var lastRemoteAuthFrame: Long = -1L

    // Last known remote input (for prediction). Starts as NONE.
    private var lastRemoteInput: InputState = InputState.NONE

    /**
     * Advance the simulation by exactly one frame.
     *
     * @param localInput the local player's raw input sampled this tick.
     * @return the new [World] (also available via [GameEngine.getWorld]).
     */
    fun tick(localInput: InputState): World {
        val currentFrame = engine.getWorld().frameNumber

        // 1. Schedule local input. Apply INPUT_DELAY frames of intentional buffering so the
        //    peer usually has it before we need it -> this is what hides jitter without any
        //    rollback happening at all.
        val scheduledFrame = currentFrame + inputDelay
        localInputs[scheduledFrame] = localInput

        // 2. Drain remote packets and process any that diverge from our predictions.
        processIncomingRemote(currentFrame)

        // 3. Pick inputs for THIS frame.
        val localForThisFrame = localInputs[currentFrame] ?: InputState.NONE
        val remoteForThisFrame = remoteForFrame(currentFrame)
        predictedRemote.putIfAbsent(currentFrame, remoteForThisFrame)

        // 4. Snapshot the pre-step world, then step.
        worldBeforeFrame[currentFrame] = engine.getWorld()
        engine.step(buildInputs(localForThisFrame, remoteForThisFrame))

        // 5. Send a redundant window of our recent local inputs.
        broadcastLocalWindow(currentFrame)

        // 6. Compact old state we can no longer roll back into.
        compact(currentFrame)

        // 7. Notify observers that a frame completed.
        listener.onFrameAdvanced(currentFrame + 1)

        return engine.getWorld()
    }

    // --- internals -----------------------------------------------------------------------

    private data class Misprediction(
        val frame: Long,
        val predicted: InputState,
        val actual: InputState,
    )

    private fun processIncomingRemote(currentFrame: Long) {
        val drained = networkInput.drainRemoteInputs()
        if (drained.isEmpty()) return

        val earliest = ingestRemoteInputs(drained, currentFrame)
        if (earliest != null) applyRewind(earliest, currentFrame)
    }

    /**
     * Absorb a batch of authoritative remote inputs, dedupe, notify the listener about
     * each prediction evaluation, and return the earliest misprediction that forces a
     * rewind (if any).
     */
    private fun ingestRemoteInputs(
        drained: List<FramedInput>,
        currentFrame: Long,
    ): Misprediction? {
        var earliest: Misprediction? = null
        for (framed in drained) {
            val miss = absorbFramed(framed, currentFrame)
            if (miss != null && (earliest == null || miss.frame < earliest.frame)) {
                earliest = miss
            }
        }
        return earliest
    }

    /**
     * Handle a single inbound [FramedInput]: dedupe, record authoritative entry, notify
     * prediction-hit observer, and return a [Misprediction] if this input invalidates an
     * earlier predicted frame.
     */
    @Suppress("ReturnCount")
    private fun absorbFramed(
        framed: FramedInput,
        currentFrame: Long,
    ): Misprediction? {
        val frame = framed.frameNumber
        val input = framed.input

        val prior = remoteAuthoritative[frame]
        if (prior != null && prior == input) return null
        remoteAuthoritative[frame] = input
        if (frame > lastRemoteAuthFrame) lastRemoteAuthFrame = frame

        if (frame >= currentFrame) return null
        val predicted = predictedRemote[frame] ?: return null
        listener.onPredictionEvaluated(hit = predicted == input)
        return if (predicted != input) Misprediction(frame, predicted, input) else null
    }

    private fun applyRewind(
        miss: Misprediction,
        currentFrame: Long,
    ) {
        val effectiveFrom = resimulateFrom(miss.frame, currentFrame)
        if (effectiveFrom < 0) return
        listener.onRollback(
            RollbackEvent(
                currentFrame = currentFrame,
                mispredictionFrame = miss.frame,
                effectiveFromFrame = effectiveFrom,
                rewindFrames = (currentFrame - effectiveFrom).toInt(),
                authLag = (currentFrame - miss.frame).toInt(),
                predicted = miss.predicted,
                actual = miss.actual,
            ),
        )
    }

    /**
     * Rewind to [fromFrame] (clamped by [maxRollbackFrames]) and replay up to but not
     * including [currentFrame]. Returns the frame we actually restored from, or -1 if no
     * snapshot was available.
     */
    private fun resimulateFrom(
        fromFrame: Long,
        currentFrame: Long,
    ): Long {
        val oldest = (currentFrame - maxRollbackFrames).coerceAtLeast(0L)
        val effectiveFrom = fromFrame.coerceAtLeast(oldest)

        val snapshot = worldBeforeFrame[effectiveFrom] ?: return -1L
        engine.setWorld(snapshot)

        var f = effectiveFrom
        while (f < currentFrame) {
            val local = localInputs[f] ?: InputState.NONE
            val remote = remoteForFrame(f)
            predictedRemote[f] = remote
            worldBeforeFrame[f] = engine.getWorld()
            engine.step(buildInputs(local, remote))
            f++
        }
        return effectiveFrom
    }

    private fun remoteForFrame(frame: Long): InputState {
        val auth = remoteAuthoritative[frame]
        if (auth != null) {
            lastRemoteInput = auth
            return auth
        }
        // Prediction: repeat last known input. Simple and very effective for fighters,
        // where button state tends to persist across consecutive frames.
        return lastRemoteInput
    }

    private fun buildInputs(
        local: InputState,
        remote: InputState,
    ): Map<Int, InputState> = mapOf(localPlayerId to local, remotePlayerId to remote)

    private fun broadcastLocalWindow(currentFrame: Long) {
        val window = ArrayList<Pair<Long, InputState>>(sendWindow)
        val start = (currentFrame - sendWindow + 1).coerceAtLeast(0L)
        var f = start
        while (f <= currentFrame + inputDelay) {
            val input = localInputs[f] ?: break
            window.add(f to input)
            f++
        }
        if (window.isNotEmpty()) networkOutput.sendInputWindow(window)
    }

    private fun compact(currentFrame: Long) {
        // Anything older than (currentFrame - maxRollbackFrames) cannot be rolled back into,
        // so it is safe to forget. We keep one extra frame for safety.
        val cutoff = currentFrame - maxRollbackFrames - 1
        if (cutoff <= 0) return
        worldBeforeFrame.keys.removeAll { it < cutoff }
        localInputs.keys.removeAll { it < cutoff }
        remoteAuthoritative.keys.removeAll { it < cutoff }
        predictedRemote.keys.removeAll { it < cutoff }
    }

    companion object {
        const val DEFAULT_INPUT_DELAY = 2
        const val DEFAULT_MAX_ROLLBACK_FRAMES = 8
        const val DEFAULT_SEND_WINDOW = 8
    }
}
