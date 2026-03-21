package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.entity.World

// these are pure rules
@Suppress("UtilityClassWithPublicConstructor")
class GameLogicSystem {
    companion object {
        fun defaultGameLogic(state: World): World {
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
