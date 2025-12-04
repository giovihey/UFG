package com.heyteam.ufg.domain.service

class TimeManager(
    targetFPS: Int = GameConstants.TARGET_FPS,
) {
    private var lastFrameTime = System.currentTimeMillis()
    private var deltaTime = 0.0
    private var fps = 0

    private val msPerFrame = GameConstants.MS_PER_SECOND / targetFPS

    fun update(): Double {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFrameTime

        if (elapsed < msPerFrame) {
            Thread.sleep((msPerFrame - elapsed).toLong())
        }

        deltaTime = (System.currentTimeMillis() - lastFrameTime) / GameConstants.MS_PER_SECOND
        lastFrameTime = System.currentTimeMillis()

        if (fps % GameConstants.TARGET_FPS == 0) fps = GameConstants.TARGET_FPS else fps++
        return deltaTime
    }

    fun getFPS(): Int = fps
}
