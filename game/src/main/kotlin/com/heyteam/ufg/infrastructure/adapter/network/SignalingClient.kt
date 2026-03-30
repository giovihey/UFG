package com.heyteam.ufg.infrastructure.adapter.network

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

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
                    println("connected to signaling server")
                }

                override fun onMessage(message: String?) {
                    println("Signaling received: $message")
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
                    println("Disconnected from signaling server")
                }

                override fun onError(ex: Exception?) {
                    ex?.printStackTrace()
                }
            }
        ws.connectBlocking()
        bridge.signalingListener = this
    }

    override fun onLocalDescription(sdp: String) {
        if (!descriptionSent) {
            descriptionSent = true
            val json = JSONObject()
            json.put("type", "sdp")
            json.put("sdp", sdp)
            ws.send(json.toString())
        }
    }

    override fun onLocalCandidate(
        candidate: String,
        mid: String,
    ) {
        val json = JSONObject()
        json.put("type", "ice")
        json.put("candidate", candidate)
        json.put("mid", mid)
        ws.send(json.toString())
    }
}
