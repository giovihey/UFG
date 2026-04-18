package com.heyteam.ufg.domain.entity

import com.heyteam.ufg.domain.component.Attack
import com.heyteam.ufg.domain.component.AttackCommand
import com.heyteam.ufg.domain.component.Attacks
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants

data class Character(
    val id: Int,
    val name: String,
    val maxHealth: Health,
    val walkSpeed: Double,
    val jumpSpeed: Double,
    val weight: Double,
    val moveList: Map<AttackCommand, Attack>,
    val defaultHurtbox: Rectangle,
) {
    companion object {
        val DEFAULT =
            Character(
                id = 0,
                name = "Default",
                maxHealth = Health(100, 100),
                walkSpeed = GameConstants.WALK_SPEED,
                jumpSpeed = GameConstants.JUMP_INITIAL_VELOCITY,
                weight = 1.0,
                moveList = mapOf(AttackCommand.NEUTRAL_PUNCH to Attacks.JAB),
                defaultHurtbox = Rectangle(0.0, 0.0, 50.0, 80.0),
            )
    }
}
