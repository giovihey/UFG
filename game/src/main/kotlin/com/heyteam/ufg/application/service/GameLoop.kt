package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.application.port.output.RenderPort
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState

class GameLoop(
    private var gameEngine: GameEngine,
    private val inputPort: KeyboardInputPort,
    private val renderPort: RenderPort,
    private val timeManager: TimeManager,
    private val networkPort: NetworkPort,
    private val isHost: Boolean,
) {
    @Volatile private var isRunning = true

    fun stop() {
        isRunning = false
    }

    fun start() {
        isRunning = true
        while (isRunning) {
            val timeStepResult: FixedTimestepResult = timeManager.update()
            val steps = minOf(timeStepResult.steps, 1)

            repeat(steps) {
                if (!processFrame(timeStepResult)) {
                    isRunning = false
                }
            }

            if (!isRunning) break

            renderPort.render(gameEngine.getWorld())
            if (gameEngine.getWorld().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
                renderPort.shutdown()
            }
        }
    }

    private fun processFrame(timeStepResult: FixedTimestepResult): Boolean {
        val frame = gameEngine.getWorld().frameNumber
        val localInput: InputState = inputPort.pollInputState(if (isHost) 1 else 2)
        println("Sending frame $frame, steps=$timeStepResult.steps")
        networkPort.sendInput(localInput, frame)

        // block until remote input arrives for this frame
        var remoteInput = networkPort.pollRemoteInput(frame)
        while (remoteInput == null) {
            if (!networkPort.isConnected() || !isRunning) {
                println("Stopping game loop (Connected: ${networkPort.isConnected()}, Running: $isRunning)")
                return false
            }
            Thread.sleep(1)
            remoteInput = networkPort.pollRemoteInput(frame)
        }

        val inputs =
            if (isHost) {
                mapOf(1 to localInput, 2 to remoteInput)
            } else {
                mapOf(1 to remoteInput, 2 to localInput)
            }

        gameEngine.step(inputs, timeStepResult.fixedDt)
        return true
    }
}
