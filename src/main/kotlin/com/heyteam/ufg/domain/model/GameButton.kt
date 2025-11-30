package com.heyteam.ufg.domain.model

enum class GameButton(
    val bit: Int,
) {
    UP(1 shl 0),
    DOWN(1 shl 1),
    LEFT(1 shl 2),
    RIGHT(1 shl 3),
    PUNCH(1 shl 4),
    KICK(1 shl 5),
}
