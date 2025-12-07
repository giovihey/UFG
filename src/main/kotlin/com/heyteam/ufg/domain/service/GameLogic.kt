package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.Player

@Suppress("UtilityClassWithPublicConstructor")
class GameLogic {
    companion object {
        fun defaultGameLogic(state: GameState): GameState {
            if (state.gameStatus != GameStatus.RUNNING) return state

            val updatedPlayers =
                state.players.mapValues { (id, player) ->
                    updatePlayer(player)
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

        private fun updatePlayer(player: Player): Player {
            val newHealth =
                player.health.copy(
                    current = (player.health.current).coerceAtLeast(0),
                )

            return player.copy(
                health = newHealth,
            )
        }
    }
}
