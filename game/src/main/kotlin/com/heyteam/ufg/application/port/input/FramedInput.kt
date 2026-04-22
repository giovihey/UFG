package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.component.InputState

data class FramedInput(
    val frameNumber: Long,
    val input: InputState,
)
