package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

object HitDetectionSystem {
    fun update(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        var worldForUpdate = world
        // Iterate in a stable, deterministic order (by player id) so rollback re-simulation
        // produces bit-identical state regardless of the underlying Map implementation.
        val orderedIds = worldForUpdate.players.keys.sorted()
        orderedIds.forEach { attackerId ->
            val attacker = worldForUpdate.players[attackerId] ?: return@forEach
            val state = attacker.attackState ?: return@forEach
            if (state.hasLanded) return@forEach

            val hitBox = attacker.activeHitBox ?: return@forEach
            orderedIds
                .filter { it != attackerId }
                .mapNotNull { worldForUpdate.players[it] }
                .forEach { opponent ->
                    val opponentHurtBox =
                        opponent.effectiveHurtbox.copy(
                            x = opponent.topLeft.x,
                            y = opponent.topLeft.y,
                        )
                    if (hitBox.overlaps(opponentHurtBox)) {
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
        val knockbackDir = if (attacker.position.x < opponent.position.x) 1.0 else -1.0
        val knockback = state.attack.knockbackSpeed / opponent.character.weight

        val damagedOpponent =
            opponent.copy(
                health =
                    opponent.health.copy(
                        current = (opponent.health.current - state.attack.damage).coerceAtLeast(0),
                    ),
                physicsState =
                    opponent.physicsState.copy(
                        state = PlayerState.HITSTUN,
                        hitstunFramesRemaining = state.attack.hitstunFrames,
                    ),
                nextMove =
                    opponent.nextMove.copy(
                        speedX = knockback * knockbackDir,
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
