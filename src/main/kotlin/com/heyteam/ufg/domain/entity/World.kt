package com.heyteam.ufg.domain.entity

import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants.STAGE_HEIGHT
import com.heyteam.ufg.domain.config.GameConstants.STAGE_WIDTH

data class World(
    val frameNumber: Long,
    val players: Map<Int, Player>,
    val stageBounds: Rectangle = Rectangle(0.0, 0.0, STAGE_WIDTH, STAGE_HEIGHT),
    val gameStatus: GameStatus = GameStatus.RUNNING,
    val roundTimer: Int = 99, // seconds
) {
    fun getPlayer(id: Int): Player? = players[id]

    fun isPlayerInBounds(playerId: Int): Boolean {
        val player = getPlayer(playerId) ?: return false
        val playerBounds = player.hurtBox
        return stageBounds.overlaps(playerBounds)
    }

    fun isRoundOver(): Boolean = roundTimer <= 0

    fun copyWithUpdatedPlayer(
        playerId: Int,
        updatedPlayer: Player,
    ): World = copy(players = players + (playerId to updatedPlayer))

    fun copyWithUpdatedPlayers(updatedPlayers: Map<Int, Player>): World = copy(players = updatedPlayers)

    fun copyWithFrameIncrement(): World = copy(frameNumber = frameNumber + 1)
}
