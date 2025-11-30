package com.heyteam.ufg.infrastructure.adapter.input

import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.domain.model.InputState
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

val defaultKeyMap =
    mapOf(
        KeyEvent.VK_W to GameButton.UP,
        KeyEvent.VK_S to GameButton.DOWN,
        KeyEvent.VK_A to GameButton.LEFT,
        KeyEvent.VK_D to GameButton.RIGHT,
        KeyEvent.VK_P to GameButton.PUNCH,
        KeyEvent.VK_K to GameButton.KICK,
    )

class KeyboardInputAdapter(
    private val keyMap: Map<Int, GameButton>,
) : KeyAdapter() {
    companion object {
        val DEFAULT = KeyboardInputAdapter(defaultKeyMap)
    }

    @Volatile // couldbebug
    private var currentBitMask: Int = 0

    override fun keyPressed(e: KeyEvent?) {
        val gameButton = keyMap[e?.keyCode] ?: return

        currentBitMask = currentBitMask or gameButton.bit
    }

    override fun keyReleased(e: KeyEvent?) {
        val gameButton = keyMap[e?.keyCode] ?: return

        // bit.inv(): Turn the bit off when released
        currentBitMask = currentBitMask and gameButton.bit.inv()
    }

    fun getCurrentInputState(): InputState = InputState(currentBitMask)
}
