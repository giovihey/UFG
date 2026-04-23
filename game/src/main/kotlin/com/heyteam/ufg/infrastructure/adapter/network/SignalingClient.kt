package com.heyteam.ufg.infrastructure.adapter.network

import io.github.oshai.kotlinlogging.KotlinLogging
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
}
