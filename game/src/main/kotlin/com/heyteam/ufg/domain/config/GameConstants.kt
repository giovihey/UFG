package com.heyteam.ufg.domain.config

object GameConstants {
    // Movement
    const val WALK_SPEED = 120.0 // pixels/sec
    const val RUN_SPEED = 200.0 // pixels/sec
    const val JUMP_INITIAL_VELOCITY = -350.0 // pixels/sec (negative = up)

    // Gravity
    const val GRAVITY = 1000.0 // pixels/sec²
    const val MAX_FALL_SPEED = 500.0 // pixels/sec (terminal velocity)

    // Stage
    const val STAGE_MARGIN = 100.0 // pixels from edge
    const val STAGE_WIDTH = 800.0
    const val STAGE_HEIGHT = 600.0
    const val FLOOR_Y = 320.0 // Ground level
    const val COLOR_STAGE_BACKGROUND = 0xFF1A1A2E
    const val COLOR_STAGE_MARGIN = 0xFF444466
    const val COLOR_FLOOR = 0xFF2A2A4A

    const val PLAYER_DAMAGE_PER_FRAME = 25 // health points per frame

    const val UI_REFRESH_RATE = 16L // milliseconds

    // Timing
    const val TARGET_FPS = 60
    const val MS_PER_SECOND = 1000.0
    const val MILLIS_FOR_FPS = 6L

    // Game Loop
    const val ROUND_END_DELAY_MS = 2000L // 2 seconds

    const val SCALE = 0.001
    const val ACCEL_RIGHT = 3.0
    const val FRICTION = 0.88
}
