package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.World

object InputSystem {
    fun apply(
        world: World,
        input: InputState,
    ): World {
        when {
            input.isPressed(GameButton.LEFT) -> println("LEFT")
            input.isPressed(GameButton.DOWN) -> println("DOWN")
            input.isPressed(GameButton.UP) -> println("UP")
            input.isPressed(GameButton.RIGHT) -> println("RIGHT")
            input.isPressed(GameButton.KICK) -> println("KICK")
            input.isPressed(GameButton.PUNCH) -> println("PUNCH")
            input.isPressed(GameButton.JUMP) -> println("JUMP")
        }

        val p1 = world.players[1] ?: return world
        // Horizontal — independent of vertical
        val dx =
            when {
                input.isPressed(GameButton.LEFT) -> -1.0
                input.isPressed(GameButton.RIGHT) -> 1.0
                else -> 0.0
            }

        // Jump — only if pressing JUMP and currently on the ground
        val isGrounded = p1.position.y >= GameConstants.FLOOR_Y
        val newSpeedY =
            if (input.isPressed(GameButton.JUMP) && isGrounded) {
                GameConstants.JUMP_INITIAL_VELOCITY // -350.0, launches upward
            } else {
                p1.nextMove.speedY // keep current vertical speed (gravity handles it)
            }

        val updatedPlayer =
            p1.copy(
                nextMove =
                    p1.nextMove.copy(
                        direction = Direction(dx, 0.0),
                        speedY = newSpeedY,
                    ),
            )
        return world.copyWithUpdatedPlayer(1, updatedPlayer)
    }
}
