package com.heyteam.ufg.domain.model

@JvmInline
value class InputState(
    val mask: Int,
) {
    fun isPressed(button: GameButton): Boolean = (mask and button.bit) != 0

    companion object {
        val NONE = InputState(0)
    }
}
