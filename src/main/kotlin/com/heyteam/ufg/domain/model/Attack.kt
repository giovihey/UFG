package com.heyteam.ufg.domain.model

import com.heyteam.ufg.domain.physics.Rectangle
import java.awt.Button

data class Attack(
    val name: String,
    val damage: Int,
    val buttonInput: Button,
    val hitBox: Rectangle,
    val startupFrames: Int,
    val activeFrames: Int,
    val recoveryFrames: Int,
)
