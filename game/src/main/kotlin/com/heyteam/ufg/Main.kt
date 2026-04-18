package com.heyteam.ufg

import com.heyteam.ufg.application.service.CheckSessionUseCase
import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.application.service.LoginUseCase
import com.heyteam.ufg.application.service.RegisterUseCase
import com.heyteam.ufg.application.service.SessionStore
import com.heyteam.ufg.application.service.TimeManager
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.component.Screen
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter
import com.heyteam.ufg.infrastructure.adapter.network.HttpAuthAdapter
import com.heyteam.ufg.infrastructure.adapter.network.NetworkAdapter
import com.heyteam.ufg.infrastructure.adapter.network.SignalingClient
import com.heyteam.ufg.infrastructure.adapter.network.WebRtcBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val P1_START_X = 150.0
const val P2_START_X = 600.0
const val PLAYER_HURTBOX_W = 50.0
const val PLAYER_HURTBOX_H = 80.0
const val PLAYER_MAX_HEALTH = 100
const val POLL_INTERVAL_MS = 100L

// ── Service URLs ──────────────────────────────────────────────────────────
// auth-service is mapped to 8081 in docker-compose.yml (ports: "8081:8080")
// signaling stays on 8080
const val AUTH_BASE_URL = "http://localhost:8081"
const val SIGNALING_URL = "ws://localhost:8080/ws"

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
        // Menu Play button → boot WebRTC + game loop on a background thread.
        // The isHost parameter is ignored here — we use the --host arg instead.
        // This whole block will be replaced by the lobby service later.
        scope.launch(Dispatchers.IO) {
            val bridge = WebRtcBridge()
            val networkAdapter = NetworkAdapter(bridge)
            val signalingClient = SignalingClient(SIGNALING_URL, bridge)

            // Listeners ASSEMBLE!!!
            bridge.dataChannelListener = networkAdapter
            bridge.initialize("stun:stun.l.google.com:19302")
            signalingClient.connect()

            // Wait for the WebSocket handshake to complete before sending anything.
            // Without this wait, createOffer() fires before the connection is open
            // and the SDP is lost — exactly the "Cannot send local description" warning.
            var attempts = 0
            while (!signalingClient.isReady() && attempts < 20) {
                delay(250)
                attempts++
            }

            if (!signalingClient.isReady()) {
                composeAdapter.showError("Could not connect to signaling server. Is it running?")
                return@launch
            }

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
                    gameEngine = GameEngine(createWorld()),
                    inputPort = composeAdapter,
                    renderPort = composeAdapter,
                    timeManager = TimeManager(targetFPS = 60),
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
    }
    composeAdapter.startUI()
}

fun createWorld(): World {
    val p1 =
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
        )
    val p2 =
        Player(
            id = 2,
            name = "P2",
            position = Position(P2_START_X, 0.0),
            nextMove =
                Movement(
                    direction = Direction(0.0, 0.0),
                    position = Position(P2_START_X, 0.0),
                    speedX = 0.0,
                    speedY = 0.0,
                ),
            health = Health(PLAYER_MAX_HEALTH, PLAYER_MAX_HEALTH),
            hurtBox = Rectangle(P2_START_X, 0.0, PLAYER_HURTBOX_W, PLAYER_HURTBOX_H),
        )
    return World(frameNumber = 0L, players = mapOf(1 to p1, 2 to p2))
}
