package com.heyteam.ufg.infrastructure.adapter.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

private val log = KotlinLogging.logger {}

class SignalingClient(
    private val serverUrl: String,
    private val bridge: WebRtcBridge,
) : SignalingListener {
    private lateinit var ws: WebSocketClient
    private var descriptionSent = false

    // Start-frame handshake primitives. Completed exactly once when the peer signals
    // readiness / a start-at timestamp. Consumers await them via the suspend accessors.
    private val peerReady = CompletableDeferred<Unit>()
    private val startAt = CompletableDeferred<Long>()

    fun connect() {
        ws =
            object : WebSocketClient(URI(serverUrl)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    log.info { "Connected to signaling server" }
                }

                override fun onMessage(message: String?) {
                    log.debug { "Signaling received: $message" }
                    val json = JSONObject(message)
                    when (json.getString("type")) {
                        "sdp" -> bridge.setRemoteDescription(json.getString("sdp"))
                        "ice" -> bridge.addIceCandidate(json.getString("candidate"), json.getString("mid"))
                        "ready" -> peerReady.complete(Unit)
                        "start" -> startAt.complete(json.getLong("at"))
                    }
                }

                override fun onClose(
                    code: Int,
                    reason: String?,
                    remote: Boolean,
                ) {
                    log.info { "Disconnected from signaling server" }
                }

                override fun onError(ex: Exception?) {
                    log.error(ex) { "Signaling socket error" }
                }
            }
        ws.connectBlocking()
        bridge.signalingListener = this
    }

    fun isReady(): Boolean = ::ws.isInitialized && ws.isOpen

    override fun onLocalDescription(sdp: String) {
        if (!descriptionSent) {
            if (ws.isOpen) {
                descriptionSent = true
                val json = JSONObject()
                json.put("type", "sdp")
                json.put("sdp", sdp)
                ws.send(json.toString())
            } else {
                log.warn { "Cannot send local description, signaling WebSocket is not open" }
            }
        }
    }

    override fun onLocalCandidate(
        candidate: String,
        mid: String,
    ) {
        if (ws.isOpen) {
            val json = JSONObject()
            json.put("type", "ice")
            json.put("candidate", candidate)
            json.put("mid", mid)
            ws.send(json.toString())
        } else {
            log.warn { "Cannot send local candidate, signaling WebSocket is not open" }
        }
    }

    /** Tell the peer we are ready to start the match. Idempotent on retry. */
    fun sendReady() {
        if (!ws.isOpen) {
            log.warn { "Cannot send ready, signaling WebSocket is not open" }
            return
        }
        ws.send(JSONObject().put("type", "ready").toString())
    }

    /**
     * Tell the peer to begin the simulation at wall-clock [atEpochMs]. Only the host
     * should call this. Guest learns the value by awaiting [awaitStartAt].
     */
    fun sendStart(atEpochMs: Long) {
        if (!ws.isOpen) {
            log.warn { "Cannot send start, signaling WebSocket is not open" }
            return
        }
        ws.send(JSONObject().put("type", "start").put("at", atEpochMs).toString())
    }

    /** Suspends until the peer has sent `ready`. */
    suspend fun awaitPeerReady() = peerReady.await()

    /** Suspends until the peer has sent `start`, returning the agreed start epoch ms. */
    suspend fun awaitStartAt(): Long = startAt.await()
}
