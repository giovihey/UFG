package com.heyteam.ufg.domain.model

import com.heyteam.ufg.domain.physics.Rectangle

data class GameState(
    val frameNumber: Long,
    val players: Map<Int, Player>,
    val stageWidth: Double = 1000.0,
    val stageHeight: Double = 600.0,
    val floorY: Double = 0.0,
    val stageBounds: Rectangle = Rectangle(0.0, 0.0, stageWidth, stageHeight),
    val gameStatus: GameStatus = GameStatus.RUNNING,
    val roundTimer: Int = 99, // seconds
    val hitStopFrames: Int = 0,
) {
    fun getPlayer(id: Int): Player? = players[id]

    fun getOpponent(playerId: Int): Player? = players.values.firstOrNull { it.id != playerId }

    fun isPlayerInBounds(playerId: Int): Boolean {
        val player = getPlayer(playerId) ?: return false
        val playerBounds = player.hurtBox
        return stageBounds.overlaps(playerBounds)
    }

    fun isRoundOver(): Boolean = players.values.any { it.health.current <= 0 } || roundTimer <= 0

    fun copyWithUpdatedPlayer(
        playerId: Int,
        updatedPlayer: Player,
    ): GameState = copy(players = players + (playerId to updatedPlayer))

    fun copyWithUpdatedPlayers(updatedPlayers: Map<Int, Player>): GameState = copy(players = updatedPlayers)

    fun copyWithHitStop(frames: Int): GameState = copy(hitStopFrames = frames)

    fun copyWithFrameIncrement(): GameState = copy(frameNumber = frameNumber + 1)
}

enum class GameStatus {
    RUNNING,
    PAUSED,
    ROUND_END,
    MATCH_END,
}
