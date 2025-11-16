package com.heyteam.ufg.domain.model

data class Movement(
    val direction: Direction,
    val position: Position,
    val speed: Int
)
