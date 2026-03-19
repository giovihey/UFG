package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.physics.PhysicsSystem

class GameEngine(
    private val state: GameState,
    private val gameLogic: (GameState, Double) -> GameState,
    private val physicsSystem: (GameState, Double) -> GameState = PhysicsSystem::update,
) {
    fun getState(): GameState = state

    fun withState(newState: GameState): GameEngine = GameEngine(newState, gameLogic, physicsSystem)

    fun update(timeStep: FixedTimestepResult): GameEngine {
        val (_, steps, fixedDt) = timeStep
        var newState = state

        // Fixed timestep physics loop (passed from TimeManager)
        repeat(steps) {
            newState = physicsSystem(newState, fixedDt)
            newState = gameLogic(newState, fixedDt)
            newState = newState.copyWithFrameIncrement()
        }

        return GameEngine(newState, gameLogic, physicsSystem)
    }
}
