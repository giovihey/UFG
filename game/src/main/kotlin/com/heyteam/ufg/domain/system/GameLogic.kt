package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Facing
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.config.GameConstants.P1_START_X
import com.heyteam.ufg.domain.config.GameConstants.P2_START_X
import com.heyteam.ufg.domain.config.GameConstants.ROUNDS_TO_WIN
import com.heyteam.ufg.domain.config.GameConstants.ROUND_TIMER_SECONDS
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

        // Tick the round timer once per second.
        val updatedTimer =
            if (world.frameNumber > 0 && world.frameNumber % GameConstants.FRAMES_PER_SECOND == 0L) {
                (world.roundTimer - 1).coerceAtLeast(0)
            } else {
                world.roundTimer
            }
        val timed = world.copy(roundTimer = updatedTimer)

        return timed.roundWinner()?.let { winner ->
            val newWins = timed.roundWins + (winner to (timed.roundWins.getOrDefault(winner, 0) + 1))
            if (newWins.getOrDefault(winner, 0) >= ROUNDS_TO_WIN) {
                timed.copy(roundWins = newWins, gameStatus = GameStatus.MATCH_END)
            } else {
                val nextRound = timed.roundNumber + 1
                val resetPlayers =
                    timed.players.mapValues { (_, player) ->
                        val startX = if (player.id == 1) P1_START_X else P2_START_X
                        val startFacing = if (player.id == 1) Facing.RIGHT else Facing.LEFT
                        player.copy(
                            health = player.health.copy(current = player.health.max),
                            position = Position(startX, GameConstants.FLOOR_Y),
                            nextMove =
                                Movement(
                                    direction = Direction(0.0, 0.0),
                                    position = Position(startX, GameConstants.FLOOR_Y),
                                    speedX = 0.0,
                                    speedY = 0.0,
                                ),
                            attackState = null,
                            physicsState = PlayerPhysicsState(facing = startFacing),
                        )
                    }
                timed.copy(
                    players = resetPlayers,
                    roundTimer = ROUND_TIMER_SECONDS,
                    roundNumber = nextRound,
                    roundWins = newWins,
                    gameStatus = GameStatus.RUNNING,
                )
            }
        } ?: timed
    }
}
