package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.domain.component.InputState
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Decorator that holds incoming remote inputs for [lagFrames] local ticks before releasing
 * them to the consumer. On loopback, without this, rollbacks never fire because remote
 * inputs always arrive before they are needed. Inject via `--fake-lag=N` to visualise how
 * the rollback rate responds to authoritative-input delay.
 *
 * peerFrame() is also lagged through the same queue so that time-sync stalling reacts as
 * if the peer were genuinely behind, not just the input packets.
 */
class FakeLagInputPort(
    private val delegate: NetworkPort,
    private val lagFrames: Int,
) : NetworkPort {
    private data class Held(
        val releaseAtTick: Long,
        val input: FramedInput,
    )

    private val queue = ArrayDeque<Held>()
    private var tick = 0L
    private var observedPeerFrame: Long = -1L

    init {
        log.info { "FakeLagInputPort active: holding remote inputs for $lagFrames tick(s)" }
    }

    override fun pollRemoteInput(frameNumber: Long): InputState? = delegate.pollRemoteInput(frameNumber)

    override fun drainRemoteInputs(): List<FramedInput> {
        tick++
        for (f in delegate.drainRemoteInputs()) queue.addLast(Held(tick + lagFrames, f))
        // Observe peerFrame at intake time so it rides the same lag pipe as the inputs.
        val livePeerFrame = delegate.peerFrame()
        if (livePeerFrame > observedPeerFrame) observedPeerFrame = livePeerFrame
        if (queue.isEmpty()) return emptyList()
        val out = ArrayList<FramedInput>()
        while (queue.isNotEmpty() && queue.first().releaseAtTick <= tick) {
            out.add(queue.removeFirst().input)
        }
        return out
    }

    override fun peerFrame(): Long {
        // Best effort: report the peer frame minus the configured lag, so the local side
        // believes the peer is `lagFrames` behind where it actually is.
        if (observedPeerFrame < 0) return -1L
        return (observedPeerFrame - lagFrames).coerceAtLeast(0L)
    }

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    ) = delegate.sendInput(inputState, frameNumber, senderCurrentFrame, committedFrame, committedHash)

    override fun sendInputWindow(
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
        window: List<Pair<Long, InputState>>,
    ) = delegate.sendInputWindow(senderCurrentFrame, committedFrame, committedHash, window)

    override fun drainRemoteCommittedHashes() = delegate.drainRemoteCommittedHashes()

    override fun isConnected(): Boolean = delegate.isConnected()

    override fun close() = delegate.close()
}
