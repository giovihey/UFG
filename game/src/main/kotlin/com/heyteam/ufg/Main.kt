package com.heyteam.ufg

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.output.CharacterRepository
import com.heyteam.ufg.application.service.CheckSessionUseCase
import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.application.service.LoginUseCase
import com.heyteam.ufg.application.service.RegisterUseCase
import com.heyteam.ufg.application.service.SessionStore
import com.heyteam.ufg.application.service.TimeManager
import com.heyteam.ufg.domain.component.Screen
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter
import com.heyteam.ufg.infrastructure.adapter.network.FakeLagInputPort
import com.heyteam.ufg.infrastructure.adapter.network.HttpAuthAdapter
import com.heyteam.ufg.infrastructure.adapter.network.LocalNetworkPort
import com.heyteam.ufg.infrastructure.adapter.network.NetworkAdapter
import com.heyteam.ufg.infrastructure.adapter.network.SignalingClient
import com.heyteam.ufg.infrastructure.adapter.network.WebRtcBridge
import com.heyteam.ufg.infrastructure.adapter.output.JsonCharacterRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val log = KotlinLogging.logger {}

const val P1_START_X = 150.0
const val P2_START_X = 600.0
const val POLL_INTERVAL_MS = 100L
const val P1_CHARACTER_ID = 2 // rushdown
const val P2_CHARACTER_ID = 3 // heavy
const val SIGNALING_CONNECT_TIMEOUT_MS = 250L
const val SIGNALING_CONNECT_MAX_ATTEMPTS = 20
const val TARGET_FPS = 60

const val START_BUFFER_MS = 500L
const val HANDSHAKE_TIMEOUT_MS = 5_000L

// Minimum time the VS splash is visible, regardless of how long the sync wait takes.
const val VS_SPLASH_MIN_MS = 2_000L

const val AUTH_BASE_URL = "http://localhost:8081"
const val SIGNALING_URL = "ws://localhost:8080/ws"
const val STUN_SERVER = "stun:stun.l.google.com:19302"

fun main(args: Array<String>) {
    val isHost = args.contains("--host")
    val fakeLag =
        args
            .firstOrNull { it.startsWith("--fake-lag=") }
            ?.removePrefix("--fake-lag=")
            ?.toIntOrNull()
            ?.coerceAtLeast(0)
            ?: 0

    val scope = CoroutineScope(Dispatchers.Default)

    val composeAdapter = ComposeAdapter()
    val authAdapter = HttpAuthAdapter(AUTH_BASE_URL)
    val sessionStore = SessionStore()

    val checkSessionUseCase = CheckSessionUseCase(sessionStore, composeAdapter)
    val loginUseCase = LoginUseCase(authAdapter, sessionStore, composeAdapter)
    val registerUseCase = RegisterUseCase(authAdapter, sessionStore, composeAdapter)

    composeAdapter.onPlayPressed = {
        checkSessionUseCase.execute()
    }

    composeAdapter.onLogin = { username, password ->
        scope.launch { loginUseCase.execute(username, password) }
    }

    composeAdapter.onRegister = { username, password ->
        scope.launch { registerUseCase.execute(username, password) }
    }

    composeAdapter.onGameStart = { _ ->
        scope.launch(Dispatchers.IO) {
            onGameStart(composeAdapter, isHost, fakeLag, sessionStore)
        }
    }

    composeAdapter.startUI()
}

/**
 * Full matchmaking + game-start flow:
 *
 *  1. Spin up a [LocalNetworkPort] practice loop immediately → player can warm up while
 *     we negotiate the WebRTC connection in the background.
 *  2. Once the peer's data channel opens, stop the practice loop and run the start-frame
 *     handshake.
 *  3. Navigate to [Screen.VsSplash] for the duration of the [START_BUFFER_MS] sync wait.
 *  4. At the agreed epoch, swap in the real [GameLoop] and navigate to [Screen.Game].
 *
 * No domain or application-layer code was changed to support this — only a new adapter
 * ([LocalNetworkPort]) and two new screens ([PracticeScreen], [VsSplashScreen]).
 */
private suspend fun onGameStart(
    composeAdapter: ComposeAdapter,
    isHost: Boolean,
    fakeLag: Int,
    sessionStore: SessionStore,
) {
    val characters: CharacterRepository = JsonCharacterRepository()
    val practiceLoop = createPracticeLoop(composeAdapter, characters)
    startPracticeLoop(composeAdapter, practiceLoop)

    val (networkPort, signalingClient, networkAdapter) =
        setupNetworkAndSignaling(isHost, fakeLag, composeAdapter, practiceLoop)

    practiceLoop.stop()

    val startAt =
        runHandshake(signalingClient, isHost) ?: return run {
            composeAdapter.showError("Peer did not respond to start handshake.")
            networkAdapter.close()
        }

    showVsSplashAndWait(composeAdapter, sessionStore, isHost, startAt)

    val realLoop = createRealGameLoop(composeAdapter, characters, networkPort, isHost)
    startRealGameLoop(composeAdapter, realLoop, networkAdapter, startAt)
}

private fun createPracticeLoop(
    composeAdapter: ComposeAdapter,
    characters: CharacterRepository,
): GameLoop =
    GameLoop(
        gameEngine = GameEngine(createWorld(characters)),
        inputPort = composeAdapter,
        renderPort = composeAdapter,
        timeManager = TimeManager(targetFPS = TARGET_FPS),
        networkPort = LocalNetworkPort(),
        isHost = true, // P1 is always local in practice
    )

private fun startPracticeLoop(
    composeAdapter: ComposeAdapter,
    practiceLoop: GameLoop,
) {
    composeAdapter.onShutdown = { composeAdapter.navigate(Screen.Menu) }
    Thread(practiceLoop::start, "practice-loop").apply { isDaemon = true }.start()
    composeAdapter.navigate(Screen.Practice)
    log.info { "Practice loop started, searching for peer..." }
}

private suspend fun setupNetworkAndSignaling(
    isHost: Boolean,
    fakeLag: Int,
    composeAdapter: ComposeAdapter,
    practiceLoop: GameLoop,
): Triple<NetworkPort, SignalingClient, NetworkAdapter> {
    val bridge = WebRtcBridge()
    val networkAdapter = NetworkAdapter(bridge)
    val networkPort = if (fakeLag > 0) FakeLagInputPort(networkAdapter, fakeLag) else networkAdapter
    val signalingClient = SignalingClient(SIGNALING_URL, bridge)

    bridge.dataChannelListener = networkAdapter
    bridge.initialize(STUN_SERVER)
    signalingClient.connect()

    var attempts = 0
    while (!signalingClient.isReady() && attempts < SIGNALING_CONNECT_MAX_ATTEMPTS) {
        delay(SIGNALING_CONNECT_TIMEOUT_MS)
        attempts++
    }

    if (!signalingClient.isReady()) {
        practiceLoop.stop()
        composeAdapter.showError("Could not connect to signaling server. Is it running?")
        error("Signaling connection failed")
    }

    log.info { "Signaling ready. isHost=$isHost" }
    if (isHost) bridge.createOffer()

    while (!networkAdapter.isConnected()) {
        delay(POLL_INTERVAL_MS)
    }
    log.info { "Peer connected. Running start-frame handshake..." }

    return Triple(networkPort, signalingClient, networkAdapter)
}

private suspend fun showVsSplashAndWait(
    composeAdapter: ComposeAdapter,
    sessionStore: SessionStore,
    isHost: Boolean,
    startAt: Long,
) {
    val localName = sessionStore.get()?.username ?: "P1"
    val remoteName = if (isHost) "P2" else "P1"
    val splashShownAt = System.currentTimeMillis()
    composeAdapter.navigate(Screen.VsSplash(p1Name = localName, p2Name = remoteName))

    val targetMs = maxOf(startAt, splashShownAt + VS_SPLASH_MIN_MS)
    val sleepMs = (targetMs - System.currentTimeMillis()).coerceAtLeast(0L)
    if (sleepMs > 0) delay(sleepMs)
}

private fun createRealGameLoop(
    composeAdapter: ComposeAdapter,
    characters: CharacterRepository,
    networkPort: NetworkPort,
    isHost: Boolean,
): GameLoop =
    GameLoop(
        gameEngine = GameEngine(createWorld(characters)),
        inputPort = composeAdapter,
        renderPort = composeAdapter,
        timeManager = TimeManager(targetFPS = TARGET_FPS),
        networkPort = networkPort,
        isHost = isHost,
    )

private fun startRealGameLoop(
    composeAdapter: ComposeAdapter,
    realLoop: GameLoop,
    networkAdapter: NetworkAdapter,
    startAt: Long,
) {
    composeAdapter.onShutdown = {
        realLoop.stop()
        networkAdapter.close()
    }

    log.info { "Starting real game loop at epoch=$startAt" }
    Thread(realLoop::start, "game-loop").apply { isDaemon = true }.start()
    composeAdapter.navigate(Screen.Game)
}

/**
 *  1. Both peers send `ready`.
 *  2. Both peers wait for the peer's `ready`.
 *  3. Host picks `at = now + START_BUFFER_MS` and broadcasts `start`. Guest awaits it.
 *
 */
private suspend fun runHandshake(
    signalingClient: SignalingClient,
    isHost: Boolean,
): Long? {
    signalingClient.sendReady()
    val peerReady = withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) { signalingClient.awaitPeerReady() }
    if (peerReady == null) {
        log.warn { "Handshake timeout: peer never sent 'ready'" }
        return null
    }
    return if (isHost) {
        val at = System.currentTimeMillis() + START_BUFFER_MS
        signalingClient.sendStart(at)
        at
    } else {
        withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) { signalingClient.awaitStartAt() }
            ?: run {
                log.warn { "Handshake timeout: host never sent 'start'" }
                null
            }
    }
}
