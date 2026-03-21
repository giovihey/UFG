package com.heyteam.ufg.domain.entity

data class Character(
    val id: Int,
    val name: String,
    val maxHealth: Health,
    val moveList: Map<MoveInput, Attack>,
    val defaultHurtbox: Rectangle,
)
