package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.domain.system.GameLogic

class GameEngine(
    private var world: World,
) {
    fun getWorld(): World = world

    fun step(
        inputState: InputState,
        fixedDt: Double,
    ) {
        world = GameLogic.step(world, inputState, fixedDt)
    }
}
