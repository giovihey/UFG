package com.heyteam.ufg.domain.model

class GameState(
    val frameNumber: Long,
    val players: Map<Int, Player>,
    val stageWidth: Double = 1000.0,
    val floorY: Double = 0.0,
) {
    fun getPlayer(id: Int): Player? = players[id]
}
