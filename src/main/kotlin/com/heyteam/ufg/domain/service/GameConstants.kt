package com.heyteam.ufg.domain.service

// Game Configuration Constants
object GameConstants {
    // Physics
    const val PLAYER_MOVE_SPEED = 100.0 // pixels per second
    const val PLAYER_DAMAGE_PER_FRAME = 25 // health points per frame
    const val STAGE_MARGIN = 100.0 // pixels from edge

    // Timing
    const val TARGET_FPS = 60
    const val MS_PER_SECOND = 1000.0

    // Game Loop
    const val ROUND_END_DELAY_MS = 2000L // 2 seconds

    const val SCALE = 0.001
    const val GRAVITY = 1200.0
    const val ACCEL_RIGHT = 300.0
    const val FRICTION = 0.88
}
