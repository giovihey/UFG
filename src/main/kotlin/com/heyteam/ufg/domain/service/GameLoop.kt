package com.heyteam.ufg.domain.service

import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.InputState
import com.heyteam.ufg.domain.model.RenderPort
import com.heyteam.ufg.domain.service.GameConstants.MILLIS_FOR_FPS
import com.heyteam.ufg.domain.service.GameConstants.TARGET_FPS

class GameLoop(
    private var gameEngine: GameEngine,
    private val inputPort: KeyboardInputPort,
    private val renderPort: RenderPort,
) {
    private val fixedDt = 1.0 / 60.0
    private var accumulator = 0.0

    fun start() {
        var isRunning = true
        while (isRunning) {
            val step = FixedTimestepResult(1.0 / TARGET_FPS, 1, 1.0 / TARGET_FPS)
            val nextState = applyInputToState(gameEngine.getState(), inputPort.pollInputState(1))
            gameEngine = gameEngine.withState(nextState).update(step)
            renderPort.render(gameEngine.getState())
            if (gameEngine.getState().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
            }
            Thread.sleep(MILLIS_FOR_FPS)
        }
    }

    private fun applyInputToState(
        state: GameState,
        input: InputState,
    ): GameState {
        when {
            input.isPressed(GameButton.LEFT) -> println("LEFT")
            input.isPressed(GameButton.DOWN) -> println("DOWN")
            input.isPressed(GameButton.UP) -> println("UP")
            input.isPressed(GameButton.RIGHT) -> println("RIGHT")
            else -> Direction(0.0, 0.0)
        }
        val p1 = state.players[1] ?: return state
        val direction =
            when {
                input.isPressed(GameButton.LEFT) -> Direction(-1.0, 0.0)
                input.isPressed(GameButton.RIGHT) -> Direction(1.0, 0.0)
                else -> Direction(0.0, 0.0)
            }
        return state.copyWithUpdatedPlayer(1, p1.copy(nextMove = p1.nextMove.copy(direction = direction)))
    }
}
