package com.heyteam.ufg.domain.model

data class Player(
    val id: Int,
    val name: String,
    val position: Position,
    val nextMove: Movement,
    val life: Double,
)
