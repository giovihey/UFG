package com.heyteam.ufg.domain.component

sealed class Screen {
    object Title : Screen()

    object Auth : Screen()

    object Menu : Screen()

    object Game : Screen()
}
