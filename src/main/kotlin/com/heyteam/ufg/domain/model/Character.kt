package com.heyteam.ufg.domain.model

import com.heyteam.ufg.domain.physics.Rectangle

data class Character(
    val id: Int,
    val name: String,
    val maxHealth: Health,
    val moveList: Map<MoveInput, Attack>,
    val defaultHurtbox: Rectangle,
)
