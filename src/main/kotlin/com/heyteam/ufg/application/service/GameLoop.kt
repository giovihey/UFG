package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.application.port.output.RenderPort
import com.heyteam.ufg.domain.entity.Direction
import com.heyteam.ufg.domain.entity.GameButton
import com.heyteam.ufg.domain.entity.GameState
import com.heyteam.ufg.domain.entity.GameStatus
import com.heyteam.ufg.domain.entity.InputState
import com.heyteam.ufg.domain.service.TimeManager

class GameLoop(
    private var gameEngine: GameEngine,
    private val inputPort: KeyboardInputPort,
    private val renderPort: RenderPort,
    private val timeManager: TimeManager,
) {
    fun start() {
        var isRunning = true
        while (isRunning) {
            val step = timeManager.update()
            val nextState = applyInputToState(gameEngine.getState(), inputPort.pollInputState(1))
            gameEngine = gameEngine.withState(nextState).update(step)
            renderPort.render(gameEngine.getState())
            if (gameEngine.getState().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
            }
            timeManager.update()
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
