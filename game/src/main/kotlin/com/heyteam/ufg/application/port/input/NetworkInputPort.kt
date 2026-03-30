package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.component.InputState

interface NetworkInputPort {
    fun pollRemoteInput(frameNumber: Long): InputState?
}
