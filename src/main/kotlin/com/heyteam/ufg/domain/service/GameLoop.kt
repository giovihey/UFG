package com.heyteam.ufg.domain.service

import com.heyteam.ufg.application.port.input.InputPort
import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.InputState
import com.heyteam.ufg.domain.model.RenderPort

class GameLoop(
    private val timeManager: TimeManager,
    private var gameEngine: GameEngine,
    private val inputPort: InputPort,
    private val renderPort: RenderPort,
) {
    fun start() {
        var isRunning = true
        while (isRunning) {
            val step = timeManager.update()
            val nextState = applyInputToState(gameEngine.getState(), inputPort.getInputState(1))
            gameEngine = gameEngine.withState(nextState).update(step)
            renderPort.render(gameEngine.getState())
            if (gameEngine.getState().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
            }
        }
    }

    private fun applyInputToState(
        state: GameState,
        input: InputState,
    ): GameState {
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
