package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.domain.model.InputState
import com.heyteam.ufg.domain.model.Player

class KeyboardInputPort {
    @Volatile
    private var currentBitMask: Int = 0

    fun press(button: GameButton) {
        currentBitMask = currentBitMask or button.bit
    }

    fun release(button: GameButton) {
        currentBitMask = currentBitMask and button.bit.inv()
    }

    fun pollInputState(player: Int): InputState = InputState(currentBitMask)
}
