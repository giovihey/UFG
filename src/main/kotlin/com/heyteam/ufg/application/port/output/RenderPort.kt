package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.entity.GameState

interface RenderPort {
    fun render(gameState: GameState)
}
