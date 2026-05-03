package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.component.InputState

interface NetworkOutputPort {
    /**
     * Single-frame send.
     *
     * @param senderCurrentFrame   sender's live sim frame at the moment of the call.
     * @param committedFrame       the frame [committedHash] is over, or [NO_COMMITTED_HASH]
     *                             if the sender has no committed hash yet.
     * @param committedHash        canonical [com.heyteam.ufg.application.service.WorldHash]
     *                             of the sender's [committedFrame]. Ignored when
     *                             [committedFrame] is [NO_COMMITTED_HASH].
     */
    fun sendInput(
        inputState: InputState,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    )

    /**
     * Rollback-friendly send: transmit a redundant window of the most recent local inputs.
     * [committedFrame] / [committedHash] piggyback once per packet (same value on every
     * frame in the window) and let the peer detect cross-machine desyncs — see
     * [com.heyteam.ufg.application.service.RollbackService].
     */
    fun sendInputWindow(
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
        window: List<Pair<Long, InputState>>,
    ) {
        window.forEach { (frame, input) ->
            sendInput(input, frame, senderCurrentFrame, committedFrame, committedHash)
        }
    }

    companion object {
        /** Sentinel: sender has no committed hash to share yet. */
        const val NO_COMMITTED_HASH: Long = Long.MIN_VALUE
    }
}
