package com.heyteam.ufg

import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.infrastructure.adapter.input.KeyboardInputAdapter

fun main() {
    fun Int.getActivatedButtons(): Set<GameButton> = GameButton.entries.filter { (this and it.bit) != 0 }.toSet()
    println("Fight in UFG")
    val input: KeyboardInputAdapter = KeyboardInputAdapter.DEFAULT

    while (true) {
        val currInput = input.getCurrentInputState()
        println(currInput.mask)
        val activeButtons = currInput.mask.getActivatedButtons()
        println("Pulsanti premuti: ${activeButtons.joinToString(", ") { it.name }}")
        Thread.sleep(1000L)
    }
}
