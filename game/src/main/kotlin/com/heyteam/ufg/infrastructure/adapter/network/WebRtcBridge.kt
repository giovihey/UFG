package com.heyteam.ufg.infrastructure.adapter.network

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

@Suppress("TooManyFunctions")
class WebRtcBridge : PeerConnectionBridge {
    companion object {
        init {
            System.loadLibrary("webrtc_wrapper") // loads libwebrtc_wrapper.dylib
        }
    }

    var signalingListener: SignalingListener? = null
    var dataChannelListener: DataChannelListener? = null

    // called from C++ when local SDP is ready
    fun onLocalDescription(sdp: String) {
        log.debug { "C++ fired onLocalDescription" }
        signalingListener?.onLocalDescription(sdp)
    }

    fun onLocalCandidate(
        candidate: String,
        mid: String,
    ) {
        log.debug { "C++ fired onLocalCandidate" }
        signalingListener?.onLocalCandidate(candidate, mid)
    }

    fun onDataChannelOpen() {
        log.debug { "C++ fired onDataChannelOpen" }
        dataChannelListener?.onDataChannelOpen()
    }

    fun onDataChannelClose() {
        log.debug { "C++ fired onDataChannelClose" }
        dataChannelListener?.onDataChannelClose()
    }

    fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    ) {
        dataChannelListener?.onRemoteInput(
            inputMask,
            frameNumber,
            senderCurrentFrame,
            committedFrame,
            committedHash,
        )
    }

    // These match the C++ functions exactly
    external override fun initialize(stunServer: String)

    external override fun createOffer(): String

    external override fun setRemoteDescription(sdp: String)

    external override fun addIceCandidate(
        candidate: String,
        mid: String,
    )

    // Wire format documented in PeerConnectionBridge.sendInput. The native side must pack
    // and unpack 36 bytes — any mismatch with this signature scrambles frame numbers and
    // causes immediate desync.
    external override fun sendInput(
        inputMask: Int,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    )

    external override fun close()
}
