package com.heyteam.ufg.domain.entity

import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle

data class Player(
    val id: Int,
    val name: String,
    val position: Position,
    val nextMove: Movement,
    val health: Health,
    val hurtBox: Rectangle,
    val physicsState: PlayerPhysicsState = PlayerPhysicsState(),
    val attackState: AttackState? = null,
) {
    // The top-left corner for rendering and collision, derived from position
    val topLeft: Position
        get() =
            Position(
                x = position.x - hurtBox.width / 2, // centered horizontally
                y = position.y - hurtBox.height, // above the feet
            )
}
