package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.output.AuthPort
import com.heyteam.ufg.application.port.output.ScreenPort
import com.heyteam.ufg.domain.component.Screen

class LoginUseCase(
    private val authPort: AuthPort,
    private val sessionStore: SessionStore,
    private val screenPort: ScreenPort,
) {
    suspend fun execute(
        username: String,
        password: String,
    ) {
        authPort
            .login(username, password)
            .onSuccess { session ->
                sessionStore.save(session)
                screenPort.navigate(Screen.Menu)
            }.onFailure { error ->
                // return error to the UI — screen decides how to show it
                screenPort.showError(error.message ?: "Login failed")
            }
    }
}
