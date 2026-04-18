package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.component.Screen

interface ScreenPort {
    fun navigate(screen: Screen)

    fun back()

    fun showError(message: String)
}
