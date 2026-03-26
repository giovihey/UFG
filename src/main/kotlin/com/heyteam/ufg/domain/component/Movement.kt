package com.heyteam.ufg.domain.component

import com.heyteam.ufg.domain.component.Direction

data class Movement(
    val direction: Direction,
    val position: Position,
    val speedX: Double,
    val speedY: Double,
)
