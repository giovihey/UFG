package com.heyteam.ufg.domain.entity

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants.ROUND_TIMER_SECONDS
import com.heyteam.ufg.domain.config.GameConstants.STAGE_HEIGHT
import com.heyteam.ufg.domain.config.GameConstants.STAGE_WIDTH

data class World(
    val frameNumber: Long,
    val players: Map<Int, Player>,
    val stageBounds: Rectangle = Rectangle(0.0, 0.0, STAGE_WIDTH, STAGE_HEIGHT),
    val gameStatus: GameStatus = GameStatus.RUNNING,
    val roundTimer: Int = ROUND_TIMER_SECONDS,
    val roundNumber: Int = 1, // 1-indexed, max 3
    val roundWins: Map<Int, Int> = mapOf(1 to 0, 2 to 0), // playerId → wins
) {
    fun getPlayer(id: Int): Player? = players[id]

    fun isPlayerInBounds(playerId: Int): Boolean {
        val player = getPlayer(playerId) ?: return false
        val playerBounds = player.hurtBox
        return stageBounds.overlaps(playerBounds)
    }

    /**
     * Returns the id of the player who wins this round right now, or null if the
     * round is still in progress.
     *
     * KO: a player's health reached 0 → the other player wins.
     * Time-out: timer hit 0 → the player with more health wins (tie = P1 wins).
     */
    fun roundWinner(): Int? {
        val p1 = players[1]
        val p2 = players[2]
        return when {
            p1 != null && p1.health.current <= 0 -> 2
            p2 != null && p2.health.current <= 0 -> 1
            roundTimer <= 0 -> {
                val p1hp = p1?.health?.current ?: 0
                val p2hp = p2?.health?.current ?: 0
                if (p1hp >= p2hp) 1 else 2
            }
            else -> null
        }
    }

    fun copyWithUpdatedPlayer(
        playerId: Int,
        updatedPlayer: Player,
    ): World = copy(players = players + (playerId to updatedPlayer))

    fun copyWithUpdatedPlayers(updatedPlayers: Map<Int, Player>): World = copy(players = updatedPlayers)

    fun copyWithFrameIncrement(): World = copy(frameNumber = frameNumber + 1)
}
