package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState

class GameEngine(
    private val state: GameState,
    private val gameLogic: (GameState, Double) -> GameState,
) {
    fun getState(): GameState = state

    fun update(deltaTime: Double): GameEngine {
        val newState =
            if (state.hitStopFrames > 0) {
                state
                    .copyWithHitStop(state.hitStopFrames - 1)
                    .copyWithFrameIncrement()
            } else {
                // Apply game logic
                gameLogic(state, deltaTime)
                    .copyWithFrameIncrement()
            }

        return GameEngine(newState, gameLogic)
    }
}
