package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.component.InputState

interface NetworkOutputPort {
    // Legacy single-frame send, still used for backwards-compat call sites.
    fun sendInput(
        inputState: InputState,
        frameNumber: Long,
    )

    // Rollback-friendly send: transmit a redundant window of the most recent local inputs.
    // Each element is (absolute frame number, input at that frame). Loss of any single
    // packet is masked by the next one, so no explicit retransmission is needed.
    fun sendInputWindow(window: List<Pair<Long, InputState>>) {
        window.forEach { (frame, input) -> sendInput(input, frame) }
    }
}
