package com.heyteam.ufg.domain.component

// It's a placeholder for testing the attack system
object Attacks {
    val JAB =
        Attack(
            name = "Jab",
            damage = 5,
            buttonInput = GameButton.PUNCH,
            hitBox = Rectangle(25.0, 5.0, 40.0, 30.0), // offset applied later
            startupFrames = 4,
            activeFrames = 3,
            recoveryFrames = 8,
        )
}
