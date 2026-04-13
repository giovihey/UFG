package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.World

object InputSystem {
    fun apply(
        world: World,
        inputs: Map<Int, InputState>,
    ): World {
        var nextWorld = world
        for ((id, player) in world.players) {
            val input = inputs[id] ?: InputState.NONE

            // Horizontal movement
            val dx =
                when {
                    input.isPressed(GameButton.LEFT) -> -1.0
                    input.isPressed(GameButton.RIGHT) -> 1.0
                    else -> 0.0
                }

            // Jump
            val isGrounded = player.position.y >= GameConstants.FLOOR_Y
            val newSpeedY =
                if (input.isPressed(GameButton.JUMP) && isGrounded) {
                    GameConstants.JUMP_INITIAL_VELOCITY
                } else {
                    player.nextMove.speedY
                }

            val updatedPlayer =
                player.copy(
                    nextMove =
                        player.nextMove.copy(
                            direction = player.nextMove.direction.copy(x = dx),
                            speedY = newSpeedY,
                        ),
                )
            nextWorld = nextWorld.copyWithUpdatedPlayer(id, updatedPlayer)
        }
        return nextWorld
    }
}
