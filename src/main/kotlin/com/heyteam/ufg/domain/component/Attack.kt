package com.heyteam.ufg.domain.component

data class Attack(
    val name: String,
    val damage: Int,
    val buttonInput: GameButton,
    val hitBox: Rectangle,
    val startupFrames: Int,
    val activeFrames: Int,
    val recoveryFrames: Int,
)
