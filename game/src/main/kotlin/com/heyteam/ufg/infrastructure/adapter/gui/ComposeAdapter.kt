package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.heyteam.ufg.application.port.output.ScreenPort
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.Screen
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.infrastructure.adapter.gui.screen.authScreen
import com.heyteam.ufg.infrastructure.adapter.gui.screen.gameScreen
import com.heyteam.ufg.infrastructure.adapter.gui.screen.menuScreen
import com.heyteam.ufg.infrastructure.adapter.gui.screen.titleScreen

class ComposeAdapter :
    RenderPort,
    KeyboardInputPort,
    ScreenPort {
    @Volatile private var currentBitMask: Int = 0
    private var currentScreen by mutableStateOf<Screen>(Screen.Title)
    private var errorMessage by mutableStateOf<String?>(null)
    var onPlayPressed: () -> Unit = {}
    var onLogin: (username: String, password: String) -> Unit = { _, _ -> }
    var onRegister: (username: String, password: String) -> Unit = { _, _ -> }
    var onGameStart: (isHost: Boolean) -> Unit = {}
    var onShutdown: (() -> Unit)? = null

    // mutableStateOf is thread-safe for reads/writes
    // mutableStateOf is the magic — unlike @Volatile, Compose observes it. When the game loop
    // writes a new World, Compose sees the change and redraws the screen.
    private var worldState by mutableStateOf<World?>(null)

    override fun render(world: World) {
        worldState = world
    }

    override fun shutdown() {
        onShutdown?.invoke()
    }

    private val defaultKeyMap: Map<Key, GameButton> =
        mapOf(
            Key.W to GameButton.UP,
            Key.A to GameButton.LEFT,
            Key.S to GameButton.DOWN,
            Key.D to GameButton.RIGHT,
            Key.P to GameButton.PUNCH,
            Key.K to GameButton.KICK,
            Key.Spacebar to GameButton.JUMP,
        )

    fun startUI() =
        application {
            val windowState =
                rememberWindowState(
                    size = DpSize(800.dp, 600.dp),
                    position = WindowPosition(Alignment.Center),
                )

            Window(
                onCloseRequest = {
                    shutdown()
                    exitApplication()
                },
                title = "UFG",
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
                when (currentScreen) {
                    // Play pressed → Main.kt decides: Auth or Menu
                    is Screen.Title -> {
                        titleScreen(
                            onPlay = { onPlayPressed() },
                        )
                    }

                    is Screen.Auth -> {
                        authScreen(
                            errorMessage = errorMessage,
                            onLogin = { u, p -> onLogin(u, p) },
                            onRegister = { u, p -> onRegister(u, p) },
                        )
                    }

                    // Menu: Play triggers game start — Main.kt decides host/guest
                    // This will later be replaced by the lobby flow
                    is Screen.Menu -> {
                        menuScreen(
                            onPlay = { onGameStart(true) }, // temporary — lobby replaces this
                            onOptions = { /* future */ },
                            onHelp = { /* future */ },
                        )
                    }

                    // Game screen: driven by world state from the game loop
                    is Screen.Game -> {
                        val world = worldState
                        if (world != null) gameScreen(world)
                    }
                }
            }
        }

    // Input handling
    override fun press(button: GameButton) {
        currentBitMask = currentBitMask or button.bit
    }

    override fun release(button: GameButton) {
        currentBitMask = currentBitMask and button.bit.inv()
    }

    override fun pollInputState(player: Int): InputState = InputState(currentBitMask)

    override fun navigate(screen: Screen) {
        currentScreen = screen
    }

    override fun back() {
        currentScreen = Screen.Title
    }

    override fun showError(message: String) {
        errorMessage = message
        navigate(Screen.Auth)
    }
}
