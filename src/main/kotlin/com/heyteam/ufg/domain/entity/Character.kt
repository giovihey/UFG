package com.heyteam.ufg.domain.entity

import com.heyteam.ufg.domain.component.Attack
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.MoveInput
import com.heyteam.ufg.domain.component.Rectangle

data class Character(
    val id: Int,
    val name: String,
    val maxHealth: Health,
    val moveList: Map<MoveInput, Attack>,
    val defaultHurtbox: Rectangle,
)
