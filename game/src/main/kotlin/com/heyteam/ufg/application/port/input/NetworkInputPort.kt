package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.component.InputState

interface NetworkInputPort {
    // Legacy delay-based API: returns and removes a single frame's input if already received.
    // Retained for compatibility; rollback code uses drainRemoteInputs() instead.
    fun pollRemoteInput(frameNumber: Long): InputState?

    // Non-blocking: return every authoritative remote input packet received since the last drain.
    // Order is unspecified; callers must key by frame number.
    fun drainRemoteInputs(): List<FramedInput>
}
