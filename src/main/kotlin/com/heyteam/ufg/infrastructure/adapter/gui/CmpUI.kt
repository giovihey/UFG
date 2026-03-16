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
import com.heyteam.ufg.domain.service.FixedTimestepResult
import com.heyteam.ufg.domain.service.GameConstants
import com.heyteam.ufg.domain.service.GameEngine
import com.heyteam.ufg.domain.service.GameLogic
import com.heyteam.ufg.domain.service.TimeManager
import kotlinx.coroutines.delay

// ── UI presentation constants ─────────────────────────────────────────────────

private const val UI_POLL_INTERVAL_MS = 16L
private const val FLOOR_BAR_HEIGHT_DP = 4
private const val CONTENT_PADDING_DP = 16
private const val DEBUG_PADDING_DP = 12
private const val CARD_ELEVATION_DP = 4

// ── Initial player / character data ──────────────────────────────────────────

private const val P1_START_X = 100.0
private const val PLAYER_HURTBOX_W = 50.0
private const val PLAYER_HURTBOX_H = 80.0
private const val PLAYER_MAX_HEALTH = 100
private const val CHARACTER_DEFAULT_HURTBOX_W = 60.0
private const val CHARACTER_DEFAULT_HURTBOX_H = 100.0

private val P1_COLOR = Color.Blue
private val P2_COLOR = Color.Red
private val FLOOR_COLOR = Color.Green

// ── Entry point ───────────────────────────────────────────────────────────────

fun startUI() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "UFG – Fighting Game") {
            MaterialTheme { gameApp() }
        }
    }

// ── Root composable ───────────────────────────────────────────────────────────

@Suppress("Indentation")
@Composable
fun gameApp() {
    val runner = remember { GameRunner(createInitialEngine()) }
    var gameState by remember { mutableStateOf(runner.stateSnapshot) }
    var inputMask by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runner.start() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(UI_POLL_INTERVAL_MS)
            gameState = runner.stateSnapshot
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    val bit = keyToBit(event.key) ?: return@onKeyEvent false
                    val isDown = event.type == KeyEventType.KeyDown
                    val newMask = if (isDown) inputMask or bit else inputMask and bit.inv()
                    if (newMask == inputMask) return@onKeyEvent false
                    inputMask = newMask
                    runner.setInput(InputState(newMask))
                    true
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            gameHeader(gameState)
            stageView(gameState)
            debugPanel(gameState, InputState(inputMask))
            Button(
                modifier = Modifier.padding(top = CONTENT_PADDING_DP.dp),
                onClick = {
                    runner.reset()
                    inputMask = 0
                },
            ) { Text("Reset") }
        }
    }
}

// ── Game header ───────────────────────────────────────────────────────────────

@Composable
private fun gameHeader(state: GameState) {
    Column(
        modifier = Modifier.padding(CONTENT_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Frame ${state.frameNumber}",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
        )
        Text("${state.gameStatus}  |  Timer: ${state.roundTimer}s")
    }
}

// ── Stage view ────────────────────────────────────────────────────────────────

@Suppress("Indentation")
@Composable
private fun stageView(state: GameState) {
    Card(
        modifier =
            Modifier
                .size(GameConstants.STAGE_WIDTH.dp, GameConstants.STAGE_HEIGHT.dp)
                .padding(vertical = CONTENT_PADDING_DP.dp),
        elevation = CARD_ELEVATION_DP.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(FLOOR_BAR_HEIGHT_DP.dp)
                        .align(Alignment.BottomStart)
                        .background(FLOOR_COLOR),
            )
            state.players.values.forEach { player -> playerSprite(player) }
        }
    }
}

// ── Player sprite ─────────────────────────────────────────────────────────────

@Suppress("Indentation")
@Composable
private fun playerSprite(player: Player) {
    val spriteW = player.hurtBox.width
    val spriteH = player.hurtBox.height
    val uiX = player.position.x.dp
    val uiY = (GameConstants.STAGE_HEIGHT - player.position.y - spriteH).dp
    val color = if (player.id == 1) P1_COLOR else P2_COLOR

    Card(
        modifier =
            Modifier
                .size(spriteW.dp, spriteH.dp)
                .offset(x = uiX, y = uiY),
        backgroundColor = color,
        elevation = CARD_ELEVATION_DP.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "P${player.id}\n${player.health.current}HP",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Debug panel ───────────────────────────────────────────────────────────────

@Suppress("Indentation")
@Composable
private fun debugPanel(
    state: GameState,
    input: InputState,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(CONTENT_PADDING_DP.dp)) {
        Column(modifier = Modifier.padding(DEBUG_PADDING_DP.dp)) {
            Text("Input", style = MaterialTheme.typography.h6)
            Row {
                GameButton.entries.forEach { btn ->
                    Text("${btn.name}: ${if (input.isPressed(btn)) "●" else "○"}  ")
                }
            }
            state.players[1]?.let { p ->
                Text(
                    text =
                        "P1  x=${p.position.x.toInt()}  y=${p.position.y.toInt()}" +
                            "  vx=${"%.2f".format(p.nextMove.speedX)}" +
                            "  vy=${"%.2f".format(p.nextMove.speedY)}",
                )
            }
        }
    }
}

// ── Input mapping ─────────────────────────────────────────────────────────────

private fun keyToBit(key: Key): Int? =
    when (key) {
        Key.A -> GameButton.LEFT.bit
        Key.D -> GameButton.RIGHT.bit
        Key.W -> GameButton.UP.bit
        Key.S -> GameButton.DOWN.bit
        Key.U -> GameButton.PUNCH.bit
        Key.I -> GameButton.KICK.bit
        else -> null
    }

private fun applyInputToState(
    state: GameState,
    input: InputState,
): GameState {
    val p1 = state.players[1] ?: return state
    val direction =
        when {
            input.isPressed(GameButton.LEFT) -> Direction(-1.0, 0.0)
            input.isPressed(GameButton.RIGHT) -> Direction(1.0, 0.0)
            else -> Direction(0.0, 0.0)
        }
    return state.copyWithUpdatedPlayer(1, p1.copy(nextMove = p1.nextMove.copy(direction = direction)))
}

// ── Engine factory ────────────────────────────────────────────────────────────

@Suppress("Indentation")
private fun createInitialEngine(): GameEngine {
    val character =
        Character(
            id = 1,
            name = "Ryu",
            maxHealth = Health(PLAYER_MAX_HEALTH, PLAYER_MAX_HEALTH),
            moveList = emptyMap(),
            defaultHurtbox = Rectangle(0.0, 0.0, CHARACTER_DEFAULT_HURTBOX_W, CHARACTER_DEFAULT_HURTBOX_H),
        )
    val player =
        Player(
            id = 1,
            name = "P1",
            position = Position(P1_START_X, 0.0),
            nextMove =
                Movement(
                    direction = Direction(0.0, 0.0),
                    position = Position(P1_START_X, 0.0),
                    speedX = 0.0,
                    speedY = 0.0,
                ),
            health = Health(PLAYER_MAX_HEALTH, PLAYER_MAX_HEALTH),
            hurtBox = Rectangle(P1_START_X, 0.0, PLAYER_HURTBOX_W, PLAYER_HURTBOX_H),
            character = character,
        )
    val initialState = GameState(frameNumber = 0L, players = mapOf(1 to player))
    return GameEngine(
        state = initialState,
        gameLogic = { s, _ -> GameLogic.defaultGameLogic(s) },
        physicsSystem = PhysicsSystem::update,
    )
}

// ── Game runner ───────────────────────────────────────────────────────────────

/**
 * Runs the game engine on a dedicated daemon thread and exposes a thread-safe
 * snapshot of the latest [GameState] for the UI to poll.
 */
class GameRunner(
    private val initialEngine: GameEngine,
) {
    private val lock = Any()
    private var engine: GameEngine = initialEngine

    @Volatile private var input: InputState = InputState.NONE

    @Volatile private var running = false

    private val timeManager = TimeManager()
    private val gameLogic: (GameState, Double) -> GameState = { s, _ -> GameLogic.defaultGameLogic(s) }
    private val physicsSystem: (GameState, Double) -> GameState = PhysicsSystem::update

    val stateSnapshot: GameState
        get() = synchronized(lock) { engine.getState() }

    fun start() {
        if (running) return
        running = true
        Thread(::gameLoop, "ufg-game-loop").apply { isDaemon = true }.start()
    }

    fun stop() {
        running = false
    }

    fun setInput(newInput: InputState) {
        input = newInput
    }

    fun reset() =
        synchronized(lock) {
            engine = initialEngine
            input = InputState.NONE
        }

    private fun tick(step: FixedTimestepResult) =
        synchronized(lock) {
            val stateWithInput = applyInputToState(engine.getState(), input)
            engine = GameEngine(stateWithInput, gameLogic, physicsSystem).update(step)
        }

    // timeManager.update() may sleep for frame-rate limiting — kept outside the lock
    // so the UI thread is never blocked waiting for a frame boundary.
    private fun gameLoop() {
        while (running) {
            val step = timeManager.update()
            tick(step)
        }
    }
}
