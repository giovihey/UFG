package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.GameStatus

class GameLoop(
    private val timeManager: TimeManager,
    private var gameEngine: GameEngine,
) {
    private var isRunning = false

    fun start() {
        isRunning = true
        println("UFG Fighting Game Loop Started (60 FPS)")

        while (isRunning) {
            val deltaTime = timeManager.update()
            gameEngine = gameEngine.update(deltaTime)
            render()

            // Exit after round end
            if (gameEngine.getState().gameStatus == GameStatus.ROUND_END) {
                Thread.sleep(GameConstants.ROUND_END_DELAY_MS)
                stop()
            }
        }
    }

    fun render() {
        val state = gameEngine.getState()
        println("\n=== FRAME ${state.frameNumber} (${timeManager.getFPS()} FPS) ===")
        println("⏱️  Round: ${state.roundTimer}s | ${state.gameStatus}")

        state.players.forEach { (id, player) ->
            val status = if (player.health.current <= 0) "💀" else "❤️"
            val moveDir = player.nextMove.direction
            println(
                "P$id $status ${player.name} | HP=${player.health.current}/${player.health.max} | Pos=(${"%.1f".format(
                    player.position.x,
                )}, ${"%.1f".format(player.position.y)}) | Move=$moveDir",
            )
        }

        if (state.hitStopFrames > 0) println("⏸️  HIT STOP: ${state.hitStopFrames}f")
    }

    fun stop() {
        isRunning = false
        println("Match Over!")
    }
}
