package com.heyteam.ufg

import com.heyteam.ufg.application.service.GameEngine
import com.heyteam.ufg.application.service.GameLoop
import com.heyteam.ufg.application.service.TimeManager
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.infrastructure.adapter.gui.ComposeAdapter
import com.heyteam.ufg.infrastructure.adapter.network.NetworkAdapter
import com.heyteam.ufg.infrastructure.adapter.network.SignalingClient
import com.heyteam.ufg.infrastructure.adapter.network.WebRtcBridge
import kotlin.system.exitProcess

// ── Initial player / character data ──────────────────────────────────────────
const val P1_START_X = 150.0
const val P2_START_X = 600.0
const val PLAYER_HURTBOX_W = 50.0
const val PLAYER_HURTBOX_H = 80.0
const val PLAYER_MAX_HEALTH = 100
const val POLL_INTERVAL_MS = 100L

fun main(args: Array<String>) {
    val bridge = WebRtcBridge()
    val networkAdapter = NetworkAdapter(bridge)
    val signalingServerDefault: String = "ws://localhost:8080/ws"
    val signalingClient = SignalingClient(signalingServerDefault, bridge)

    // Listeners ASSEMBLE!!!
    bridge.dataChannelListener = networkAdapter
    signalingClient.connect()

    if (!signalingClient.isReady()) {
        println("ERROR: Could not connect to signaling server at $signalingServerDefault")
        println("Make sure the signaling server is running.")
        exitProcess(1)
    }

    bridge.initialize("stun:stun.l.google.com:19302")

    val isHost = args.contains("--host")

    if (isHost) {
        bridge.createOffer()
    }

    val composeAdapter = ComposeAdapter()
    val timeManager = TimeManager(targetFPS = 60)
    val engine = GameEngine(createWorld())

    println("Waiting for peer to connect...")
    while (!networkAdapter.isConnected()) {
        Thread.sleep(POLL_INTERVAL_MS)
    }
    println("Connected! Starting game.")

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
        // We can add more cleanup here if needed
    }

    Thread(loop::start, "game-loop").apply { isDaemon = true }.start()

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
