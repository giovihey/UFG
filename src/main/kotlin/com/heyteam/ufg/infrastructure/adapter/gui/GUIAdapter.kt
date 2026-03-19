package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.RenderPort

class GUIAdapter : RenderPort {
    @Volatile private var latestState: GameState? = null

    override fun render(state: GameState) {
        latestState = state
    }

    fun startUI() =
        application {
            val windowState =
                rememberWindowState(
                    size = DpSize(800.dp, 600.dp),
                    position = WindowPosition(Alignment.Center),
                )

            Window(
                onCloseRequest = ::exitApplication,
                title = "My App",
                state = windowState,
            ) {
                gameApp()
            }
        }

    @Composable
    fun gameApp() {
        counter()
    }

    @Composable
    fun greetings() {
        val name = "mounir"
        Text("Hello, $name!")
    }

    @Composable
    fun counter() {
        var count by remember { mutableStateOf(0) }

        Button(onClick = { count++ }) {
            Text("Count is now: $count")
        }
    }
}
