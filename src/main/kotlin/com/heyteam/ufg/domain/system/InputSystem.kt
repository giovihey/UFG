package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
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
        }
        val p1 = world.players[1] ?: return world
        val direction =
            when {
                input.isPressed(GameButton.LEFT) -> Direction(-1.0, 0.0)
                input.isPressed(GameButton.RIGHT) -> Direction(1.0, 0.0)
                else -> Direction(0.0, 0.0)
            }
        return world.copyWithUpdatedPlayer(1, p1.copy(nextMove = p1.nextMove.copy(direction = direction)))
    }
}
