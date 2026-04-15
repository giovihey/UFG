package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.entity.World

object AttackSystem {
    fun update(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        val updatedPlayers =
            world.players.mapValues { (_, player) ->
                val state = player.attackState ?: return@mapValues player

                if (state.isExpired) {
                    player.copy(
                        attackState = null,
                        physicsState = player.physicsState.copy(state = PlayerState.IDLE),
                    )
                } else {
                    player.copy(
                        attackState = state.copy(currentFrame = state.currentFrame + 1),
                        physicsState = player.physicsState.copy(state = PlayerState.ATTACKING),
                    )
                }
            }

        return world.copyWithUpdatedPlayers(updatedPlayers)
    }
}
