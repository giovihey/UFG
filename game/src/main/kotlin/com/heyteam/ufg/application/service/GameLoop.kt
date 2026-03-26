package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.application.port.output.RenderPort
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState

class GameLoop(
    private var gameEngine: GameEngine,
    private val inputPort: KeyboardInputPort,
    private val renderPort: RenderPort,
    private val timeManager: TimeManager,
) {
    fun start() {
        var isRunning = true
        while (isRunning) {
            val timeStepResult: FixedTimestepResult = timeManager.update()
            val inputState: InputState = inputPort.pollInputState(1)
            repeat(timeStepResult.steps) {
                gameEngine.step(inputState, timeStepResult.fixedDt)
            }
            renderPort.render(gameEngine.getWorld())
            if (gameEngine.getWorld().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
            }
        }
    }
}
