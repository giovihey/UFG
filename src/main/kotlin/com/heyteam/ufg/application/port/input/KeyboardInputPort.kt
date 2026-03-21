package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.entity.GameButton
import com.heyteam.ufg.domain.entity.InputState

interface KeyboardInputPort {
    fun press(button: GameButton)

    fun release(button: GameButton)

    fun pollInputState(player: Int): InputState
}
