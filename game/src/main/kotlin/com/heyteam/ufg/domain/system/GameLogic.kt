package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.entity.World

// these are pure rules
object GameLogic {
    fun step(
        world: World,
        inputs: Map<Int, InputState>,
        fixedDt: Double,
    ): World {
        var next = InputSystem.apply(world, inputs)
        next = PhysicsSystem.update(next, fixedDt)
        next = applyGameRules(next)
        next = next.copyWithFrameIncrement()
        return next
    }

    private fun applyGameRules(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world
        return if (world.isRoundOver()) {
            world.copy(gameStatus = GameStatus.ROUND_END)
        } else {
            world
        }
    }
}
