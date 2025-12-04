package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.Player

@Suppress("UtilityClassWithPublicConstructor")
class GameLogic {
    companion object {
        fun defaultGameLogic(
            state: GameState,
            deltaTime: Double,
        ): GameState {
            if (state.gameStatus != GameStatus.RUNNING) return state

            val updatedPlayers =
                state.players.mapValues { (id, player) ->
                    updatePlayer(player, state, deltaTime)
                }

            val finalState =
                state
                    .copyWithUpdatedPlayers(updatedPlayers)
                    .copyWithFrameIncrement()
            return if (finalState.isRoundOver()) {
                finalState.copy(gameStatus = GameStatus.ROUND_END)
            } else {
                finalState
            }
        }

        private fun updatePlayer(
            player: Player,
            state: GameState,
            deltaTime: Double,
        ): Player {
            val moveDeltaX = GameConstants.PLAYER_MOVE_SPEED * deltaTime
            val newHealth =
                player.health.copy(
                    current = (player.health.current - GameConstants.PLAYER_DAMAGE_PER_FRAME).coerceAtLeast(0),
                )
            val newPosition =
                player.position.copy(
                    x = (player.position.x + moveDeltaX).coerceIn(0.0, state.stageWidth - GameConstants.STAGE_MARGIN),
                )

            val newHurtBox = player.hurtBox.copy(x = newPosition.x, y = newPosition.y)

            return player.copy(
                position = newPosition,
                hurtBox = newHurtBox,
                health = newHealth,
            )
        }
    }
}
