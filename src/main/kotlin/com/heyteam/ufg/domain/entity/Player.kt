package com.heyteam.ufg.domain.entity

data class Player(
    val id: Int,
    val name: String,
    val position: Position,
    val nextMove: Movement,
    val health: Health,
    val hurtBox: Rectangle,
)
