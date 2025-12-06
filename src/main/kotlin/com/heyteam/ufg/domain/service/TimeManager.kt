package com.heyteam.ufg.domain.service

class TimeManager(
    targetFPS: Int = GameConstants.TARGET_FPS,
) {
    private var lastFrameTime = System.currentTimeMillis()
    private var fps = 0
    private val msPerFrame = GameConstants.MS_PER_SECOND / targetFPS.toDouble()

    // Fixed timestep management
    private var accumulator = 0.0
    val fixedDt = 1.0 / GameConstants.TARGET_FPS // Physics timestep (can differ from render FPS)

    fun update(): FixedTimestepResult {
        val currentTime = System.currentTimeMillis()
        var elapsed = (currentTime - lastFrameTime) / GameConstants.MS_PER_SECOND

        // Cap frame rate
        if (elapsed < msPerFrame) {
            Thread.sleep(((msPerFrame - elapsed) * GameConstants.MS_PER_SECOND).toLong())
            elapsed = (System.currentTimeMillis() - lastFrameTime) / GameConstants.MS_PER_SECOND
        }

        // Update accumulator
        accumulator += elapsed

        if (fps > GameConstants.TARGET_FPS) fps = 0 else fps++
        lastFrameTime = System.currentTimeMillis()

        return FixedTimestepResult(elapsed, accumulator, fixedDt)
    }

    fun getFPS(): Int = fps
}

data class FixedTimestepResult(
    val deltaTime: Double, // Real frame time (for rendering)
    val accumulator: Double, // Current accumulator (for GameEngine)
    val fixedDt: Double, // Fixed physics timestep
)
