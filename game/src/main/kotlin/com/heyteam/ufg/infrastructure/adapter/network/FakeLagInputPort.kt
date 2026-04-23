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

    init {
        log.info { "FakeLagInputPort active: holding remote inputs for $lagFrames tick(s)" }
    }

    override fun pollRemoteInput(frameNumber: Long): InputState? = delegate.pollRemoteInput(frameNumber)

    override fun drainRemoteInputs(): List<FramedInput> {
        tick++
        for (f in delegate.drainRemoteInputs()) queue.addLast(Held(tick + lagFrames, f))
        if (queue.isEmpty()) return emptyList()
        val out = ArrayList<FramedInput>()
        while (queue.isNotEmpty() && queue.first().releaseAtTick <= tick) {
            out.add(queue.removeFirst().input)
        }
        return out
    }

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
    ) = delegate.sendInput(inputState, frameNumber)

    override fun sendInputWindow(window: List<Pair<Long, InputState>>) = delegate.sendInputWindow(window)

    override fun isConnected(): Boolean = delegate.isConnected()

    override fun close() = delegate.close()
}
