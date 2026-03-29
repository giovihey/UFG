package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

object HitDetectionSystem {
    fun update(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        var worldForUpdate = world
        worldForUpdate.players.values.forEach { attacker ->
            // skip this attacker, continue loop
            val state = attacker.attackState ?: return@forEach
            if (state.hasLanded) return@forEach

            val hitBox = state.activeHitBox(attacker.position) ?: return@forEach
            worldForUpdate.players.values
                .filter { it.id != attacker.id }
                .forEach { opponent ->
                    if (hitBox.overlaps(opponent.hurtBox)) {
                        worldForUpdate = applyHit(worldForUpdate, attacker, opponent, state)
                    }
                }
        }
        return worldForUpdate
    }

    private fun applyHit(
        world: World,
        attacker: Player,
        opponent: Player,
        state: AttackState,
    ): World {
        val damagedOpponent =
            opponent.copy(
                health =
                    opponent.health.copy(
                        current = (opponent.health.current - state.attack.damage).coerceAtLeast(0),
                    ),
            )
        val updatedAttacker =
            attacker.copy(
                attackState = state.copy(hasLanded = true),
            )
        return world
            .copyWithUpdatedPlayer(opponent.id, damagedOpponent)
            .copyWithUpdatedPlayer(attacker.id, updatedAttacker)
    }
}
