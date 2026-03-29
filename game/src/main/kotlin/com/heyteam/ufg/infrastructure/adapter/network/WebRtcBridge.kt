package com.heyteam.ufg.infrastructure.adapter.network

class WebRtcBridge : PeerConnectionBridge {
    companion object {
        init {
            System.loadLibrary("webrtc_wrapper") // loads libwebrtc_wrapper.dylib
        }
    }

    // These match the C++ functions exactly
    external override fun initialize(stunServer: String)

    external override fun createOffer(): String

    external override fun setRemoteDescription(sdp: String)

    external override fun addIceCandidate(
        candidate: String,
        mid: String,
    )

    external override fun sendInput(
        inputMask: Int,
        frameNumber: Long,
    )
}
