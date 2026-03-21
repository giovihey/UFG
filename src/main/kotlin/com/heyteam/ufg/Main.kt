package com.heyteam.ufg

import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.domain.entity.Direction
import com.heyteam.ufg.domain.entity.GameState
import com.heyteam.ufg.domain.entity.Health
import com.heyteam.ufg.domain.entity.Movement
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.Position
import com.heyteam.ufg.domain.entity.Rectangle
import com.heyteam.ufg.domain.service.GameLogic
import com.heyteam.ufg.domain.service.TimeManager
import com.heyteam.ufg.domain.system.PhysicsSystem
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter

// ── Initial player / character data ──────────────────────────────────────────
const val P1_START_X = 100.0
const val PLAYER_HURTBOX_W = 50.0
const val PLAYER_HURTBOX_H = 80.0
const val PLAYER_MAX_HEALTH = 100

fun main() {
    var currentBitMask = 0
    val composeAdapter = ComposeAdapter(currentBitMask)
    val timeManager = TimeManager()

    val gameLoop =
        GameLoop(
            gameEngine = createInitialEngine(),
            inputPort = composeAdapter,
            renderPort = composeAdapter,
            timeManager = timeManager,
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
