package com.heyteam.ufg.domain.service

class TimeManager(
    targetFPS: Int = GameConstants.TARGET_FPS,
) {
    private var lastFrameTime = System.currentTimeMillis()
    private var fps = 0
    private val msPerFrame = GameConstants.MS_PER_SECOND / targetFPS.toDouble()

    // Fixed timestep management
    private var accumulatorSec = 0.0
    val fixedDt = 1.0 / GameConstants.TARGET_FPS // Physics timestep (can differ from render FPS)

    fun update(): FixedTimestepResult {
        val currentTime = System.currentTimeMillis()
        var elapsed = (currentTime - lastFrameTime).toDouble()

        // Cap frame rate
        if (elapsed < msPerFrame) {
            val sleep = (msPerFrame - elapsed).toLong().coerceAtLeast(0L)
            Thread.sleep(sleep)
            val now = System.currentTimeMillis()
            elapsed = (now - lastFrameTime).toDouble()
        }

        val elapsedSec = elapsed / GameConstants.MS_PER_SECOND
        // Accumulate elapsed time in seconds
        accumulatorSec += elapsedSec
        var steps = 0
        while (accumulatorSec >= fixedDt) {
            accumulatorSec -= fixedDt
            steps++
        }

        if (fps > GameConstants.TARGET_FPS) fps = 0 else fps++
        lastFrameTime = System.currentTimeMillis()

        return FixedTimestepResult(
            deltaTime = elapsedSec,
            steps = steps,
            fixedDt = fixedDt,
        )
    }

    fun getFPS(): Int = fps
}
