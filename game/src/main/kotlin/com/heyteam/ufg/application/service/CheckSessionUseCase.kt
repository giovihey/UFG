package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.output.ScreenPort
import com.heyteam.ufg.domain.component.Screen

class CheckSessionUseCase(
    private val sessionStore: SessionStore,
    private val screenPort: ScreenPort,
) {
    fun execute() {
        if (sessionStore.get() != null) {
            screenPort.navigate(Screen.Menu)
        } else {
            screenPort.navigate(Screen.Auth)
        }
    }
}
