package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

object PhysicsSystem {
    fun update(
        state: World,
        dt: Double,
    ): World {
        if (state.gameStatus != GameStatus.RUNNING) return state

        val updatedPlayers =
            state.players.mapValues { (_, player) ->
                updatePlayerPhysics(player, dt)
            }

        return state.copyWithUpdatedPlayers(updatedPlayers)
    }

    private fun updatePlayerPhysics(
        player: Player,
        dt: Double,
    ): Player {
        // Apply gravity
        val (newSpeedY, newY) =
            applyGravityAndCollision(
                speedY = player.nextMove.speedY,
                posY = player.position.y,
                dt = dt,
                isGrounded = (player.position.y >= GameConstants.FLOOR_Y),
            )

        // Apply horizontal movement
        val newSpeedX = player.nextMove.direction.x * GameConstants.WALK_SPEED
        val newX =
            (player.position.x + newSpeedX * dt).coerceIn(
                GameConstants.STAGE_MARGIN + player.hurtBox.width / 2,
                GameConstants.STAGE_WIDTH - GameConstants.STAGE_MARGIN - player.hurtBox.width / 2,
            )

        val isGrounded = newY >= GameConstants.FLOOR_Y

        val newPhysicsState =
            player.physicsState.copy(
                state = determineState(player, isGrounded),
            )

        return player.copy(
            position = Position(newX, newY),
            nextMove =
                player.nextMove.copy(
                    speedX = newSpeedX,
                    speedY = newSpeedY,
                ),
            physicsState = newPhysicsState,
        )
    }

    private fun applyGravityAndCollision(
        speedY: Double,
        posY: Double,
        dt: Double,
        isGrounded: Boolean,
    ): Pair<Double, Double> {
        var newSpeedY = speedY
        var newY = posY

        // Apply gravity
        if (!isGrounded) {
            newSpeedY += GameConstants.GRAVITY * dt
            newSpeedY = newSpeedY.coerceAtMost(GameConstants.MAX_FALL_SPEED)
        }

        // Update position
        newY += newSpeedY * dt

        // Collision with floor
        if (newY >= GameConstants.FLOOR_Y) {
            newY = GameConstants.FLOOR_Y
            newSpeedY = 0.0
        }

        return Pair(newSpeedY, newY)
    }

    private fun determineState(
        player: Player,
        isGrounded: Boolean,
    ): PlayerState =
        when {
            !isGrounded -> PlayerState.JUMPING
            player.nextMove.direction.x != 0.0 -> PlayerState.WALKING
            else -> PlayerState.IDLE
        }
}
