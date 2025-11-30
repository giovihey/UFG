package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameState

class GameEngine(
    private val gameState: GameState,
) {
    val players = gameState.players.values.toList()

//    fun update(deltaTime: Double) {
//        players.forEach { player ->
//        }
//    }
}
