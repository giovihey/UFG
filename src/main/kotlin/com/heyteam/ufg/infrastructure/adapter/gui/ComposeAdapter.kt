package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.heyteam.ufg.application.port.input.KeyboardInputPort
import com.heyteam.ufg.application.port.output.RenderPort
import com.heyteam.ufg.domain.entity.GameButton
import com.heyteam.ufg.domain.entity.GameState
import com.heyteam.ufg.domain.entity.InputState

class ComposeAdapter(
    private var currentBitMask: Int,
) : RenderPort,
    KeyboardInputPort {
    @Volatile private var latestState: GameState? = null

    override fun render(gameState: GameState) {
        latestState = gameState
    }

    private val defaultKeyMap: Map<Key, GameButton> =
        mapOf(
            Key.W to GameButton.UP,
            Key.A to GameButton.LEFT,
            Key.S to GameButton.DOWN,
            Key.D to GameButton.RIGHT,
            Key.P to GameButton.PUNCH,
            Key.K to GameButton.KICK,
        )

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
                onPreviewKeyEvent = { event ->
                    val button = defaultKeyMap[event.key]
                    if (button != null) {
                        when (event.type) {
                            KeyEventType.KeyDown -> press(button)
                            KeyEventType.KeyUp -> release(button)
                        }
                        true
                    } else {
                        false
                    }
                },
            ) {
                gameApp()
            }
        }

    @Composable
    fun gameApp() {
        background()
        counter()
    }

    @Composable
    fun character() {
        val name = "mounir"
        Text("Hello, $name!")
    }

    @Composable
    fun background() {
        val color: Color = Color.Blue
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color),
        )
    }

    @Composable
    fun counter() {
        var count by remember { mutableStateOf(0) }

        Button(onClick = { count++ }) {
            Text("Count is now: $count")
        }
    }

    override fun press(button: GameButton) {
        currentBitMask = currentBitMask or button.bit
    }

    override fun release(button: GameButton) {
        currentBitMask = currentBitMask and button.bit.inv()
    }

    override fun pollInputState(player: Int): InputState = InputState(currentBitMask)
}
