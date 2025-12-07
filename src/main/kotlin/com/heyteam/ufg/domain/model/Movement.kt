package com.heyteam.ufg.domain.model

data class Movement(
    val direction: Direction,
    val position: Position,
    val speedX: Double,
    val speedY: Double,
    val accelerationX: Double = 0.0,
    val accelerationY: Double = 0.0,
)
