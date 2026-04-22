package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.output.AuthPort
import com.heyteam.ufg.application.port.output.ScreenPort
import com.heyteam.ufg.domain.component.Screen

/**
 * RegisterUseCase orchestrates the registration flow.
 * The session is stored in memory (SessionStore) for the duration of the app.
 * It's never persisted to disk.
 */
class RegisterUseCase(
    private val authPort: AuthPort,
    private val sessionStore: SessionStore,
    private val screenPort: ScreenPort,
) {
    suspend fun execute(
        username: String,
        password: String,
    ) {
        authPort
            .register(username, password)
            .onSuccess { session ->
                sessionStore.save(session)
                screenPort.navigate(Screen.Menu)
            }.onFailure { error ->
                // return error to the UI — screen decides how to show it
                screenPort.showError(error.message ?: "Registration failed")
            }
    }
}
