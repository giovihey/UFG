package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState

interface KeyboardInputPort {
    fun press(button: GameButton)

    fun release(button: GameButton)

    fun pollInputState(player: Int): InputState
}
