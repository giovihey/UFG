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

    private val localPlayerId = if (isHost) 1 else 2
    private val remotePlayerId = if (isHost) 2 else 1

    // Rollback lives in the application layer and owns input prediction, snapshot ring,
    // and the rewind-and-replay state machine. GameLoop just paces and routes IO.
    private val rollback =
        RollbackService(
            engine = gameEngine,
            networkInput = networkPort,
            networkOutput = networkPort,
            localPlayerId = localPlayerId,
            remotePlayerId = remotePlayerId,
        )

    fun stop() {
        isRunning = false
    }

    fun start() {
        isRunning = true
        while (isRunning) {
            val timeStepResult: FixedTimestepResult = timeManager.update()
            // Clamp catch-up to one simulation step per render tick: rollback already
            // handles missing remote input, so we don't need the accumulator to advance
            // more than once per wall-clock tick.
            val steps = minOf(timeStepResult.steps, 1)

            repeat(steps) {
                if (!processFrame()) {
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

    private fun processFrame(): Boolean {
        if (!networkPort.isConnected()) {
            println("Stopping game loop (peer disconnected)")
            return false
        }
        val localInput: InputState = inputPort.pollInputState(localPlayerId)
        // Non-blocking: if remote input for this frame has not arrived, RollbackService
        // predicts it (repeat last) and advances. A later authoritative packet will
        // trigger a rewind-and-replay if the prediction was wrong.
        rollback.tick(localInput)
        return true
    }
}
