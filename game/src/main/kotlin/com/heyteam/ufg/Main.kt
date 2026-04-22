package com.heyteam.ufg

import com.heyteam.ufg.application.port.output.CharacterRepository
import com.heyteam.ufg.application.service.CheckSessionUseCase
import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.application.service.LoginUseCase
import com.heyteam.ufg.application.service.RegisterUseCase
import com.heyteam.ufg.application.service.SessionStore
import com.heyteam.ufg.application.service.TimeManager
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Facing
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Screen
import com.heyteam.ufg.domain.entity.Character
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter
import com.heyteam.ufg.infrastructure.adapter.network.HttpAuthAdapter
import com.heyteam.ufg.infrastructure.adapter.network.NetworkAdapter
import com.heyteam.ufg.infrastructure.adapter.network.SignalingClient
import com.heyteam.ufg.infrastructure.adapter.network.WebRtcBridge
import com.heyteam.ufg.infrastructure.adapter.output.JsonCharacterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val P1_START_X = 150.0
const val P2_START_X = 600.0
const val POLL_INTERVAL_MS = 100L
const val P1_CHARACTER_ID = 2 // rushdown
const val P2_CHARACTER_ID = 3 // heavy
const val SIGNALING_CONNECT_TIMEOUT_MS = 250L
const val SIGNALING_CONNECT_MAX_ATTEMPTS = 20
const val TARGET_FPS = 60

// ── Service URLs ──────────────────────────────────────────────────────────
// auth-service is mapped to 8081 in docker-compose.yml (ports: "8081:8080")
// signaling stays on 8080
const val AUTH_BASE_URL = "http://localhost:8081"
const val SIGNALING_URL = "ws://localhost:8080/ws"
const val STUN_SERVER = "stun:stun.l.google.com:19302"

fun main(args: Array<String>) {
    // Read --host flag once here — used later in onGameStart.
    // Temporary until the lobby service assigns host/guest automatically.
    val isHost = args.contains("--host")

    val scope = CoroutineScope(Dispatchers.Default)

    // 1. Create adapters
    // ComposeAdapter is created first — it's needed by the use cases (ScreenPort).
    // HttpAuthAdapter is the only thing that knows the auth-service URL.
    val composeAdapter = ComposeAdapter()
    val authAdapter = HttpAuthAdapter(AUTH_BASE_URL)
    val sessionStore = SessionStore()

    // 2. Create use cases
    //  only know about ports (interfaces), never about adapters directly.
    val checkSessionUseCase = CheckSessionUseCase(sessionStore, composeAdapter)
    val loginUseCase = LoginUseCase(authAdapter, sessionStore, composeAdapter)
    val registerUseCase = RegisterUseCase(authAdapter, sessionStore, composeAdapter)

    // 3. Wire callbacks into ComposeAdapter
    // This is the ONLY place where UI events meet application logic.
    // ComposeAdapter holds lambdas — it never imports use cases.

    composeAdapter.onPlayPressed = {
        // Title screen Play button → check if already logged in
        checkSessionUseCase.execute()
    }

    composeAdapter.onLogin = { username, password ->
        // Auth screen Login button → suspend call needs a coroutine
        scope.launch { loginUseCase.execute(username, password) }
    }

    composeAdapter.onRegister = { username, password ->
        // Auth screen Register button
        scope.launch { registerUseCase.execute(username, password) }
    }

    composeAdapter.onGameStart = { _ ->
        scope.launch(Dispatchers.IO) {
            onGameStart(composeAdapter, isHost)
        }
    }
    composeAdapter.startUI()
}

/**
 * Boot WebRTC + game loop on a background thread.
 * The isHost parameter comes from the --host CLI arg.
 * This whole function will be replaced by the lobby service later.
 */
private suspend fun onGameStart(
    composeAdapter: ComposeAdapter,
    isHost: Boolean,
) {
    val bridge = WebRtcBridge()
    val networkAdapter = NetworkAdapter(bridge)
    val signalingClient = SignalingClient(SIGNALING_URL, bridge)

    // Listeners ASSEMBLE!!!
    bridge.dataChannelListener = networkAdapter
    bridge.initialize(STUN_SERVER)
    signalingClient.connect()

    // Wait for the WebSocket handshake to complete before sending anything.
    // Without this wait, createOffer() fires before the connection is open
    // and the SDP is lost — exactly the "Cannot send local description" warning.
    var attempts = 0
    while (!signalingClient.isReady() && attempts < SIGNALING_CONNECT_MAX_ATTEMPTS) {
        delay(SIGNALING_CONNECT_TIMEOUT_MS)
        attempts++
    }

    if (!signalingClient.isReady()) {
        composeAdapter.showError("Could not connect to signaling server. Is it running?")
        return
    }

    val timeManager = TimeManager(targetFPS = 60)
    val characters: CharacterRepository = JsonCharacterRepository()
    val engine = GameEngine(createWorld(characters))
    println("Connected to signaling server. isHost=$isHost")

    if (isHost) bridge.createOffer()

    // Wait for the P2P data channel to open
    println("Waiting for peer to connect...")
    while (!networkAdapter.isConnected()) {
        delay(POLL_INTERVAL_MS)
    }
    println("Peer connected! Starting game.")

    val loop =
        GameLoop(
            gameEngine = engine,
            inputPort = composeAdapter,
            renderPort = composeAdapter,
            timeManager = timeManager,
            networkPort = networkAdapter,
            isHost = isHost,
        )

    composeAdapter.onShutdown = {
        loop.stop()
        networkAdapter.close()
    }

    Thread(loop::start, "game-loop").apply { isDaemon = true }.start()

    // Switch to Game screen only once the loop is running
    composeAdapter.navigate(Screen.Game)
}

fun createWorld(characters: CharacterRepository): World {
    val p1Character = requireNotNull(characters.findById(P1_CHARACTER_ID))
    val p2Character = requireNotNull(characters.findById(P2_CHARACTER_ID))
    val p1 = spawnPlayer(id = 1, name = "P1", startX = P1_START_X, character = p1Character, facing = Facing.RIGHT)
    val p2 = spawnPlayer(id = 2, name = "P2", startX = P2_START_X, character = p2Character, facing = Facing.LEFT)
    return World(frameNumber = 0L, players = mapOf(1 to p1, 2 to p2))
}

private fun spawnPlayer(
    id: Int,
    name: String,
    startX: Double,
    character: Character,
    facing: Facing,
): Player =
    Player(
        id = id,
        name = name,
        position = Position(startX, 0.0),
        nextMove =
            Movement(
                direction = Direction(0.0, 0.0),
                position = Position(startX, 0.0),
                speedX = 0.0,
                speedY = 0.0,
            ),
        health = character.maxHealth,
        hurtBox = character.defaultHurtbox,
        character = character,
        physicsState = PlayerPhysicsState(facing = facing),
    )
