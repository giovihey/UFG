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
            state.players.mapValues { (_, player) ->
                updatePlayerPhysics(player, state, dt)
            }

        return state.copyWithUpdatedPlayers(updatedPlayers)
    }

    private fun updatePlayerPhysics(
        player: Player,
        state: GameState,
        dt: Double,
    ): Player {
        // Horizontal acceleration from input
        val accelX =
            when (player.nextMove.direction) {
                Direction(x = 1.0, y = 0.0) -> GameConstants.ACCEL_RIGHT
                Direction(x = -1.0, y = 0.0) -> -GameConstants.ACCEL_RIGHT
                else -> 0.0
            }

        // Integrate: acceleration → velocity (fixed‑point style using SCALE)
        val newVelX = player.nextMove.speedX + accelX * dt / GameConstants.SCALE
        val newVelY = player.nextMove.speedY + GameConstants.GRAVITY * dt / GameConstants.SCALE

        // Integrate: velocity → position
        val newX = player.position.x + newVelX * dt
        val newY = player.position.y + newVelY * dt

        // Stage bounds
        val stageRight = state.stageWidth - GameConstants.STAGE_MARGIN
        val floorY = state.floorY // floor is at this Y (positive‑down)

        val finalXF = newX.coerceIn(0.0, stageRight)

        // Clamp to floor: player cannot go below floorY
        val clampedY = newY.coerceAtMost(floorY)

        // Zero vertical velocity when we hit the floor coming down
        val finalVelY =
            if (newY >= floorY && newVelY > 0.0) 0.0 else newVelY

        // Apply friction
        val frictionVelX = newVelX * GameConstants.FRICTION
        val frictionVelY = finalVelY * GameConstants.FRICTION

        val newHurtBox = player.hurtBox.copy(x = finalXF, y = clampedY)

        return player.copy(
            position = Position(finalXF, clampedY),
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
