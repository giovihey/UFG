package com.heyteam.ufg.infrastructure.adapter.network

class WebRtcBridge {
    companion object {
        init {
            System.loadLibrary("webrtc_wrapper")  // loads libwebrtc_wrapper.dylib
        }
    }

    // These match the C++ functions exactly
    external fun initialize(stunServer: String)
    external fun createOffer(): String
    external fun setRemoteDescription(sdp: String)
    external fun addIceCandidate(candidate: String, mid: String)
    external fun sendInput(inputMask: Int, frameNumber: Long)
}