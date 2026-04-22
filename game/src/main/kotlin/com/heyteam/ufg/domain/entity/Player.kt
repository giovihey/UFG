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
    val character: Character = Character.DEFAULT,
) {
    val effectiveHurtbox: Rectangle
        get() =
            if (physicsState.isCrouching) {
                hurtBox.copy(height = hurtBox.height / 2)
            } else {
                hurtBox
            }

    val topLeft: Position
        get() =
            Position(
                x = position.x - effectiveHurtbox.width / 2,
                y = position.y - effectiveHurtbox.height,
            )

    val activeHitBox: Rectangle?
        get() {
            val fullTopLeft =
                Position(
                    x = position.x - hurtBox.width / 2,
                    y = position.y - hurtBox.height,
                )
            return attackState?.activeHitBox(fullTopLeft, hurtBox.width, physicsState.facing)
        }
}
