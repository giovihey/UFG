package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.domain.system.PhysicsSystem

class GameEngine(
    private val state: World,
    private val gameLogic: (World, Double) -> World,
    private val physicsSystem: (World, Double) -> World = PhysicsSystem::update,
) {
    fun getState(): World = state

    fun withState(newState: World): GameEngine = GameEngine(newState, gameLogic, physicsSystem)

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
