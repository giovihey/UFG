package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.entity.World

object HitDetectionSystem {
    fun update(
        world: World,
        // dt: Double,
    ): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        // TODO(): Implement actual hit detection when Attack system is ready
        return world
    }
}
