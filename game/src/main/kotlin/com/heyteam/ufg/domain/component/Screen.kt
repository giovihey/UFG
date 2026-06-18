package com.heyteam.ufg.domain.component

sealed class Screen {
    object Title : Screen()

    object Auth : Screen()

    object Menu : Screen()

    /** Single-player warmup shown while matchmaking runs in the background. */
    object Practice : Screen()

    /**
     * Full-screen VS splash shown after the peer connects and before the real game loop
     * starts. Displayed during the [START_BUFFER_MS] synchronisation window, so it adds
     * zero extra latency to game start.
     */
    data class VsSplash(
        val p1Name: String,
        val p2Name: String,
    ) : Screen()

    object Game : Screen()
}
