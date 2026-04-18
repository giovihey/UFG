package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackButton
import com.heyteam.ufg.domain.component.AttackCommand
import com.heyteam.ufg.domain.component.AttackDirection
import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.Facing
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

object InputSystem {
    fun apply(
        world: World,
        inputs: Map<Int, InputState>,
    ): World {
        var nextWorld = world
        for ((id, player) in world.players) {
            val updated =
                if (player.physicsState.hitstunFramesRemaining > 0) {
                    tickHitstun(player)
                } else {
                    applyInput(player, inputs[id] ?: InputState.NONE)
                }
            nextWorld = nextWorld.copyWithUpdatedPlayer(id, updated)
        }
        return nextWorld
    }

    private fun tickHitstun(player: Player): Player {
        val remaining = player.physicsState.hitstunFramesRemaining - 1
        val newState = if (remaining <= 0) PlayerState.IDLE else PlayerState.HITSTUN
        return player.copy(
            physicsState =
                player.physicsState.copy(
                    hitstunFramesRemaining = remaining,
                    state = newState,
                ),
        )
    }

    private fun applyInput(
        player: Player,
        input: InputState,
    ): Player {
        val isGrounded = player.position.y >= GameConstants.FLOOR_Y
        val isCrouching = isGrounded && input.isPressed(GameButton.DOWN)

        val dx =
            if (isCrouching) {
                0.0
            } else {
                when {
                    input.isPressed(GameButton.LEFT) -> -1.0
                    input.isPressed(GameButton.RIGHT) -> 1.0
                    else -> 0.0
                }
            }

        val newSpeedY =
            if (input.isPressed(GameButton.JUMP) && isGrounded && !isCrouching) {
                player.character.jumpSpeed
            } else {
                player.nextMove.speedY
            }

        val newAttackState = nextAttackState(player, input)
        val newFacing = updatedFacing(player.physicsState.facing, input)

        return player.copy(
            nextMove =
                player.nextMove.copy(
                    direction = player.nextMove.direction.copy(x = dx),
                    speedY = newSpeedY,
                ),
            attackState = newAttackState,
            physicsState =
                player.physicsState.copy(
                    isCrouching = isCrouching,
                    facing = newFacing,
                ),
        )
    }

    private fun updatedFacing(
        current: Facing,
        input: InputState,
    ): Facing {
        val left = input.isPressed(GameButton.LEFT)
        val right = input.isPressed(GameButton.RIGHT)
        return when {
            left && !right -> Facing.LEFT
            right && !left -> Facing.RIGHT
            else -> current
        }
    }

    private fun nextAttackState(
        player: Player,
        input: InputState,
    ): AttackState? =
        player.attackState
            ?: attackButton(input)?.let { button ->
                val direction = attackDirection(input)
                val moveList = player.character.moveList
                val move =
                    moveList[AttackCommand.of(direction, button)]
                        ?: moveList[AttackCommand.of(AttackDirection.NEUTRAL, button)]
                move?.let { AttackState(attack = it) }
            }

    private fun attackButton(input: InputState): AttackButton? =
        when {
            input.isPressed(GameButton.PUNCH) -> AttackButton.PUNCH
            input.isPressed(GameButton.KICK) -> AttackButton.KICK
            else -> null
        }

    private fun attackDirection(input: InputState): AttackDirection =
        when {
            input.isPressed(GameButton.DOWN) -> AttackDirection.DOWN
            input.isPressed(GameButton.UP) -> AttackDirection.UP
            input.isPressed(GameButton.LEFT) -> AttackDirection.LEFT
            input.isPressed(GameButton.RIGHT) -> AttackDirection.RIGHT
            else -> AttackDirection.NEUTRAL
        }
}
