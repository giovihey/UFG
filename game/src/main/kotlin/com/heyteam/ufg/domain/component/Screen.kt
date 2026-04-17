package com.heyteam.ufg.domain.component

sealed class Screen {
    object Main : Screen()

    object Login : Screen()

    object Menu : Screen()

    object Game : Screen()
}
