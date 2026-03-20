package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus

// these are pure rules
@Suppress("UtilityClassWithPublicConstructor")
class GameLogic {
    companion object {
        fun defaultGameLogic(state: GameState): GameState {
            if (state.gameStatus != GameStatus.RUNNING) return state

            val finalState =
                state
                    .copyWithFrameIncrement()
            return if (finalState.isRoundOver()) {
                finalState.copy(gameStatus = GameStatus.ROUND_END)
            } else {
                finalState
            }
        }
    }
}
