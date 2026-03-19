package com.heyteam.ufg.infrastructure.adapter.input

import com.heyteam.ufg.domain.model.GameButton
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

val defaultKeyMap: Map<Int, GameButton> =
    mapOf(
        KeyEvent.VK_W to GameButton.UP,
        KeyEvent.VK_A to GameButton.LEFT,
        KeyEvent.VK_S to GameButton.DOWN,
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

    @Volatile
    private var currentBitMask: Int = 0

    override fun keyPressed(e: KeyEvent?) {
        val gameButton = keyMap[e?.keyCode] ?: return
        currentBitMask = currentBitMask or gameButton.bit
        println(currentBitMask.toString())
    }

    override fun keyReleased(e: KeyEvent?) {
        val gameButton = keyMap[e?.keyCode] ?: return
        currentBitMask = currentBitMask and gameButton.bit.inv()
        println("just released: $currentBitMask")
    }
}
