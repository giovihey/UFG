package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.application.port.input.NetworkInputPort
import com.heyteam.ufg.application.port.output.NetworkOutputPort
import com.heyteam.ufg.application.port.output.RenderPort
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.InputState

class GameLoop(
    private var gameEngine: GameEngine,
    private val inputPort: KeyboardInputPort,
    private val renderPort: RenderPort,
    private val timeManager: TimeManager,
    private val netReceiver: NetworkInputPort,
    private val netSender: NetworkOutputPort,
) {
    fun start() {
        var isRunning = true
        while (isRunning) {
            val timeStepResult: FixedTimestepResult = timeManager.update()
            repeat(minOf(timeStepResult.steps, 1)) {
                val frame = gameEngine.getWorld().frameNumber
                val localInput: InputState = inputPort.pollInputState(1)
                println("Sending frame $frame, steps=$timeStepResult.steps")
                netSender.sendInput(localInput, frame)

                // block until remote input arrives for this frame
                // (now every network issue freezes the game, next with rollback netcode we will fix this issue)
                var remoteInput = netReceiver.pollRemoteInput(frame)
                while (remoteInput == null) {
                    Thread.sleep(1)
                    remoteInput = netReceiver.pollRemoteInput(frame)
                    if (remoteInput != null) {
                        println(remoteInput.mask)
                    }
                }

                gameEngine.step(localInput, timeStepResult.fixedDt)
            }
            renderPort.render(gameEngine.getWorld())
            if (gameEngine.getWorld().gameStatus == GameStatus.ROUND_END) {
                isRunning = false
                println("Match is over")
                renderPort.shutdown()
            }
        }
    }
}
