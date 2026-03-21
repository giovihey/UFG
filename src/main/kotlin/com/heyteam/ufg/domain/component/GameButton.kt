package com.heyteam.ufg.domain.component

enum class GameButton {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    PUNCH,
    KICK,
    ;

    val bit: Int
        get() = 1 shl ordinal
}
