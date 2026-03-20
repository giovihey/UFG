package com.heyteam.ufg

import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.Health
import com.heyteam.ufg.domain.model.Movement
import com.heyteam.ufg.domain.model.Player
import com.heyteam.ufg.domain.model.Position
import com.heyteam.ufg.domain.physics.PhysicsSystem
import com.heyteam.ufg.domain.physics.Rectangle
import com.heyteam.ufg.domain.service.GameLogic
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter

// ── Initial player / character data ──────────────────────────────────────────
const val P1_START_X = 100.0
const val PLAYER_HURTBOX_W = 50.0
const val PLAYER_HURTBOX_H = 80.0
const val PLAYER_MAX_HEALTH = 100

fun main() {
    var currentBitMask = 0
    val composeAdapter = ComposeAdapter(currentBitMask)

    val gameLoop =
        GameLoop(
            gameEngine = createInitialEngine(),
            inputPort = composeAdapter,
            renderPort = composeAdapter,
        )

    Thread(gameLoop::start, "game-loop").apply { isDaemon = true }.start()

    composeAdapter.startUI()
}

@Suppress("Indentation")
fun createInitialEngine(): GameEngine {
    val player =
        Player(
            id = 1,
            name = "P1",
            position = Position(P1_START_X, 0.0),
            nextMove =
                Movement(
                    direction = Direction(0.0, 0.0),
                    position = Position(P1_START_X, 0.0),
                    speedX = 0.0,
                    speedY = 0.0,
                ),
            health = Health(PLAYER_MAX_HEALTH, PLAYER_MAX_HEALTH),
            hurtBox = Rectangle(P1_START_X, 0.0, PLAYER_HURTBOX_W, PLAYER_HURTBOX_H),
        )
    val initialState = GameState(frameNumber = 0L, players = mapOf(1 to player))
    return GameEngine(
        state = initialState,
        gameLogic = { s, _ -> GameLogic.defaultGameLogic(s) },
        physicsSystem = PhysicsSystem::update,
    )
}
