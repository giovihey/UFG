package com.heyteam.ufg.domain.entity

class InputBuffer(
    private val capacity: Int = 60,
) {
    private val history = ArrayDeque<InputState>(capacity)

    fun push(input: InputState) {
        if (history.size >= capacity) {
            history.removeFirst()
        }
        history.addLast(input)
    }

    fun wasPressedRecently(
        button: GameButton,
        framesBack: Int,
    ): Boolean = history.takeLast(framesBack).any { it.isPressed(button) }
}
