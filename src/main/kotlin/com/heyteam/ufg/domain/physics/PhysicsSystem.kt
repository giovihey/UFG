package com.heyteam.ufg.domain.physics

import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.Player
import com.heyteam.ufg.domain.model.Position
import com.heyteam.ufg.domain.service.GameConstants

@Suppress("Indentation")
object PhysicsSystem {
    fun update(
        state: GameState,
        dt: Double,
    ): GameState {
        if (state.gameStatus != GameStatus.RUNNING) return state

        val updatedPlayers =
            state.players.mapValues { (id, player) ->
                updatePlayerPhysics(player, state, dt)
            }
        return state.copyWithUpdatedPlayers(updatedPlayers)
    }

    private fun updatePlayerPhysics(
        player: Player,
        state: GameState,
        dt: Double,
    ): Player {
        // Fixed-point acceleration
        val accelX =
            when (player.nextMove.direction) {
                Direction(x = 1.0, 0.0) -> GameConstants.ACCEL_RIGHT
                Direction(-1.0, 0.0) -> -GameConstants.ACCEL_RIGHT
                else -> 0.0
            }

        // Integrate: acceleration → velocity
        val newVelX = player.nextMove.speedX + accelX * dt / GameConstants.SCALE
        val newVelY = player.nextMove.speedY + GameConstants.GRAVITY * dt / GameConstants.SCALE

        // Integrate: velocity → position
        val newX = player.position.x + newVelX * dt
        val newY = player.position.y + newVelY * dt

        // Stage bounds (fixed-point)
        val stageRight = state.stageWidth - GameConstants.STAGE_MARGIN
        val floorY = state.floorY

        val finalXF = newX.coerceIn(0.0, stageRight)
        val finalYF = newY.coerceAtLeast(floorY)

        // Ground collision
        val finalVelY = if (finalYF >= floorY && newVelY > 0) 0.0 else newVelY

        val frictionVelX = newVelX * GameConstants.FRICTION
        val frictionVelY = finalVelY * GameConstants.FRICTION

        val newHurtBox =
            player.hurtBox.copy(
                x = finalXF,
                y = finalYF,
            )

        return player.copy(
            position =
                Position(
                    x = finalXF,
                    y = finalYF,
                ),
            nextMove =
                player.nextMove.copy(
                    speedX = frictionVelX,
                    speedY = frictionVelY,
                    accelerationX = accelX,
                    accelerationY = GameConstants.GRAVITY,
                ),
            hurtBox = newHurtBox,
        )
    }
}
