package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackPhase
import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

object HitDetectionSystem {
    fun update(world: World): World {
        if (world.gameStatus != GameStatus.RUNNING) return world

        var worldForUpdate = world
        worldForUpdate.players.values.forEach { attacker ->
            // skip this attacker, continue loop
            val state = attacker.attackState ?: return@forEach
            if (state.phase != AttackPhase.ACTIVE || state.hasLanded) return@forEach

            val hitBox = worldSpaceHitBox(attacker, state.attack.hitBox)
            worldForUpdate.players.values
                .filter { it.id != attacker.id }
                .forEach { opponent ->
                    if (hitBox.overlaps(opponent.hurtBox)) {
                        worldForUpdate = applyHit(worldForUpdate, attacker, opponent, state)
                    }
                }
        }
        return world
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

    /**
     * Attack hitboxes are defined as offsets from the player's position.
     * Translates a hitbox from local space (relative to the player)
     * to world space (absolute position on the stage).
     *
     * @param player the attacking player, used for world position
     * @param hitBox the attack's hitbox in local space
     * @return the hitbox translated to world space
     *
     * Note: does not account for facing direction yet.
     */
    private fun worldSpaceHitBox(
        player: Player,
        hitBox: Rectangle,
    ): Rectangle =
        hitBox.copy(
            x = player.position.x + hitBox.x,
            y = player.position.y + hitBox.y,
        )
}
