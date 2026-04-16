package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.Attacks
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.World

object InputSystem {
    fun apply(
        world: World,
        inputs: Map<Int, InputState>,
    ): World {
        var nextWorld = world
        for ((id, player) in world.players) {
            if (player.physicsState.hitstunFramesRemaining > 0) {
                val remaining = player.physicsState.hitstunFramesRemaining - 1
                val newState = if (remaining <= 0) PlayerState.IDLE else PlayerState.HITSTUN
                val updatedPlayer =
                    player.copy(
                        physicsState =
                            player.physicsState.copy(
                                hitstunFramesRemaining = remaining,
                                state = newState,
                            ),
                    )
                nextWorld = nextWorld.copyWithUpdatedPlayer(id, updatedPlayer)
                continue
            }

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

            val newAttackState =
                when {
                    // already attacking, ignore input
                    player.attackState != null -> player.attackState

                    input.isPressed(GameButton.PUNCH) -> AttackState(attack = Attacks.JAB)

                    else -> null
                }
            val updatedPlayer =
                player.copy(
                    nextMove =
                        player.nextMove.copy(
                            direction = player.nextMove.direction.copy(x = dx),
                            speedY = newSpeedY,
                        ),
                    attackState = newAttackState,
                )
            nextWorld = nextWorld.copyWithUpdatedPlayer(id, updatedPlayer)
        }
        return nextWorld
    }
}
