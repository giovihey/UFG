package com.heyteam.ufg.domain.component

// It's a placeholder for testing the attack system
object Attacks {
    const val X_OFFSET = 25.0
    const val Y_OFFSET = 5.0
    const val WIDTH = 40.0
    const val HEIGHT = 30.0
    val JAB =
        Attack(
            name = "Jab",
            damage = 5,
            buttonInput = GameButton.PUNCH,
            hitBox = Rectangle(X_OFFSET, Y_OFFSET, WIDTH, HEIGHT), // offset applied later
            startupFrames = 4,
            activeFrames = 3,
            recoveryFrames = 8,
            hitstunFrames = 12,
            knockbackSpeed = 150.0,
        )
}
