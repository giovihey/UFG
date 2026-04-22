package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.World

// these are pure rules
object GameLogic {
    fun step(
        world: World,
        inputs: Map<Int, InputState>,
    ): World {
        var next = InputSystem.apply(world, inputs)
        next = AttackSystem.update(next)
        next = PhysicsSystem.update(next)
        next = HitDetectionSystem.update(next)
        next = applyGameRules(next)
        next = next.copyWithFrameIncrement()
        return next
    }

    private fun applyGameRules(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        val updatedTimer =
            if (world.frameNumber > 0 && world.frameNumber % GameConstants.FRAMES_PER_SECOND == 0L) {
                (world.roundTimer - 1).coerceAtLeast(0)
            } else {
                world.roundTimer
            }

        val updatedWorld = world.copy(roundTimer = updatedTimer)

        return if (updatedWorld.isRoundOver()) {
            updatedWorld.copy(gameStatus = GameStatus.ROUND_END)
        } else {
            updatedWorld
        }
    }
}
