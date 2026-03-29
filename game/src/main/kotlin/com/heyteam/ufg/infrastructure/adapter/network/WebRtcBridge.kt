package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.domain.component.MoveInput

class WebRtcBridge : PeerConnectionBridge {
    companion object {
        init {
            System.loadLibrary("webrtc_wrapper") // loads libwebrtc_wrapper.dylib
        }
    }

    var signalingListener: SignalingListener? = null

    // called from C++ when local SDP is ready
    fun onLocalDescription(sdp: String) {
        signalingListener?.onLocalDescription(sdp)
    }

    fun onLocalCandidate(
        candidate: String,
        mid: String,
    ) {
        signalingListener?.onLocalCandidate(candidate, mid)
    }

//    fun onRemoteInput(
//        inputMask: Int,
//        frameNumber: Long,
//    ) {
//    }

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
