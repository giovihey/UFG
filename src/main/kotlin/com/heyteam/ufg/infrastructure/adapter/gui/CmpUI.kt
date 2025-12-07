package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.heyteam.ufg.domain.model.Character
import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameButton
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.Health
import com.heyteam.ufg.domain.model.InputState
import com.heyteam.ufg.domain.model.Movement
import com.heyteam.ufg.domain.model.Player
import com.heyteam.ufg.domain.model.Position
import com.heyteam.ufg.domain.physics.PhysicsSystem
import com.heyteam.ufg.domain.physics.Rectangle
import com.heyteam.ufg.domain.service.GameConstants
import com.heyteam.ufg.domain.service.GameEngine
import com.heyteam.ufg.domain.service.GameLogic
import com.heyteam.ufg.domain.service.TimeManager
import kotlinx.coroutines.delay

fun startUI() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "UFG Demo (engine + WASD)") {
            MaterialTheme {
                app()
            }
        }
    }

@Suppress("Indentation")
@Composable
fun app() {
    var inputState by remember { mutableStateOf(InputState.NONE) }
    val initialEngine = remember { createInitialEngine() }
    // Create and start GuiGameRunner once
    val runner = remember { GuiGameRunner(initialEngine) }
    LaunchedEffect(Unit) {
        runner.start()
    }

    // Poll engine state periodically for UI
    var state by remember { mutableStateOf(initialEngine.getState()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(GameConstants.UI_REFRESH_RATE) // UI refresh rate
            state = runner.getStateSnapshot()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onKeyEvent { ev ->
                    val isDown = ev.type == KeyEventType.KeyDown
                    val before = inputState
                    inputState = updateInputState(inputState, ev.key, isDown)
                    if (inputState != before) {
                        runner.setInput(inputState)
                        true
                    } else {
                        false
                    }
                }.focusable()
                .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Engine frame ${state.frameNumber}",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "Status: ${state.gameStatus}  |  Round: ${state.roundTimer}s",
                modifier = Modifier.padding(bottom = 16.dp),
            )

            stageView(state)

            debugPanel(state, inputState)

            Button(onClick = {
                runner.reset(createInitialEngine())
                inputState = InputState.NONE
            }) {
                Text("Reset")
            }
        }
    }
}

@Suppress("Indentation")
@Composable
private fun stageView(state: GameState) {
    val floorBarHeightDp = 5.dp

    Card(
        modifier =
            Modifier
                .size(GameConstants.STAGE_WIDTH.dp, GameConstants.STAGE_HEIGHT.dp)
                .padding(vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            // Floor bar at bottom
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(floorBarHeightDp)
                        .align(Alignment.BottomStart)
                        .background(Color.Green),
            )

            state.players.values.forEach { p ->
                val spriteW = p.hurtBox.width
                val spriteH = p.hurtBox.height

                val uiX = p.position.x.dp
                val uiY = (GameConstants.STAGE_HEIGHT - (p.position.y + spriteH)).dp

                Card(
                    modifier =
                        Modifier
                            .size(spriteW.dp, spriteH.dp)
                            .offset(x = uiX, y = uiY),
                    backgroundColor = if (p.id == 1) Color.Blue else Color.Red,
                    elevation = 8.dp,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("${p.name}\nHP:${p.health.current}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Suppress("Indentation")
@Composable
private fun debugPanel(
    state: GameState,
    inputState: InputState,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("InputState.mask = ${inputState.mask}", style = MaterialTheme.typography.h6)
            Row {
                Text("A←: ${if (inputState.isPressed(GameButton.LEFT)) "●" else "○"}  ")
                Text("D→: ${if (inputState.isPressed(GameButton.RIGHT)) "●" else "○"}  ")
                Text("W↑: ${if (inputState.isPressed(GameButton.UP)) "●" else "○"}  ")
                Text("S↓: ${if (inputState.isPressed(GameButton.DOWN)) "●" else "○"}")
            }
            val p1 = state.players[1]
            if (p1 != null) {
                Text(
                    "P1 Pos=(${p1.position.x.toInt()}, ${p1.position.y.toInt()}) " +
                        "VelX=${"%.1f".format(p1.nextMove.speedX)} VelY=${"%.1f".format(p1.nextMove.speedY)}",
                )
            }
        }
    }
}

// --- Engine / input helpers -------------------------------------------------------------------
@Suppress("Indentation", "MagicNumber")
private fun createInitialEngine(): GameEngine {
    val p1 =
        Player(
            id = 1,
            name = "Player1",
            position = Position(100.0, 0.0), // floor = 0.0 in your physics
            nextMove =
                Movement(
                    direction = Direction(0.0, 0.0),
                    position = Position(0.0, 0.0),
                    speedX = 0.0,
                    speedY = 0.0,
                ),
            health = Health(100, 100),
            hurtBox = Rectangle(100.0, 0.0, 50.0, 100.0),
            character =
                Character(
                    id = 1,
                    name = "Ryu",
                    maxHealth = Health(100, 100),
                    moveList = emptyMap(),
                    defaultHurtbox = Rectangle(0.0, 0.0, 60.0, 120.0),
                ),
        )

    val initialState =
        GameState(
            frameNumber = 0L,
            players = mapOf(1 to p1),
        )

    return GameEngine(
        state = initialState,
        gameLogic = { s, _ -> GameLogic.defaultGameLogic(s) },
        physicsSystem = PhysicsSystem::update,
    )
}

private fun updateInputState(
    state: InputState,
    key: Key,
    isPressed: Boolean,
): InputState {
    val bit =
        when (key) {
            Key.A -> GameButton.LEFT.bit
            Key.D -> GameButton.RIGHT.bit
            Key.W -> GameButton.UP.bit
            Key.S -> GameButton.DOWN.bit
            else -> return state
        }
    return if (isPressed) {
        InputState(state.mask or bit)
    } else {
        InputState(state.mask and bit.inv())
    }
}

// This applies InputState to a GameState (used inside GuiGameRunner)
private fun applyInputToState(
    state: GameState,
    inputState: InputState,
): GameState {
    val p1 = state.players[1] ?: return state
    val dir =
        when {
            inputState.isPressed(GameButton.LEFT) -> Direction(-1.0, 0.0)
            inputState.isPressed(GameButton.RIGHT) -> Direction(1.0, 0.0)
            else -> Direction(0.0, 0.0)
        }
    val updated = p1.copy(nextMove = p1.nextMove.copy(direction = dir))
    return state.copyWithUpdatedPlayer(1, updated)
}

// --- Game runner: uses your TimeManager + GameEngine.update on a background thread -----------

class GuiGameRunner(
    initialEngine: GameEngine,
) {
    @Volatile
    private var engine: GameEngine = initialEngine

    @Volatile
    private var running = false

    @Volatile
    private var latestInput: InputState = InputState.NONE

    private val timeManager = TimeManager() // uses your accumulator logic

    fun start() {
        if (running) return
        running = true
        Thread {
            while (running) {
                val step = timeManager.update()
                val stateWithInput = applyInputToState(engine.getState(), latestInput)
                engine =
                    GameEngine(
                        stateWithInput,
                        { s, _ -> GameLogic.defaultGameLogic(s) },
                        PhysicsSystem::update,
                    ).update(step)
            }
            println("GuiGameRunner thread stopped")
        }.start()
    }

    fun stop() {
        running = false
    }

    fun setInput(input: InputState) {
        latestInput = input
    }

    fun reset(newEngine: GameEngine) {
        engine = newEngine
        latestInput = InputState.NONE
    }

    fun getStateSnapshot(): GameState = engine.getState()
}
