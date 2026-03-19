package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.model.InputState

interface InputPort {
    fun getInputState(player: Int): InputState
}
