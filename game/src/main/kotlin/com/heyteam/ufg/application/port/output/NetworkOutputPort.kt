package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.component.InputState

interface NetworkOutputPort {
    fun sendInput(inputState: InputState, frameNumber: Long)
}