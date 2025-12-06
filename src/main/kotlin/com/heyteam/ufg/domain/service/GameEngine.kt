package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.physics.PhysicsSystem

class GameEngine(
    private val state: GameState,
    private val gameLogic: (GameState, Double) -> GameState,
    private val physicsSystem: (GameState, Double) -> GameState = PhysicsSystem::update,
) {
    fun getState(): GameState = state

    fun update(timeStep: FixedTimestepResult): GameEngine {
        val (deltaTime, accumulator, fixedDt) = timeStep
        var newState = state

        // Fixed timestep physics loop (passed from TimeManager)
        while (accumulator >= fixedDt) {
            newState =
                if (newState.hitStopFrames > 0) {
                    newState.copyWithHitStop(newState.hitStopFrames - 1)
                } else {
                    physicsSystem(newState, fixedDt)
                }
        }

        // Game logic after physics
        newState =
            if (newState.hitStopFrames == 0) {
                gameLogic(newState, fixedDt)
            } else {
                newState.copyWithFrameIncrement()
            }

        return GameEngine(newState, gameLogic, physicsSystem)
    }
}
