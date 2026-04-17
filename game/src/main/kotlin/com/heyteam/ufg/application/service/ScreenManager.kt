package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.output.ScreenPort
import com.heyteam.ufg.domain.component.Screen

class ScreenManager(
    private val screenPort: ScreenPort,
) {
    private var current: Screen = Screen.Main

    fun onPlayPressed() {
        /*val jwt = tokenStore.get()
        val next = if (jwt != null) Screen.Menu else Screen.Login
        navigate(next)*/
    }

    fun onLoginSuccess() = navigate(Screen.Menu)

    private fun navigate(screen: Screen) {
        current = screen
        screenPort.navigate(screen)
    }
}
