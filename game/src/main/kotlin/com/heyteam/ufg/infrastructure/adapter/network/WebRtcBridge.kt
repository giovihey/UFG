package com.heyteam.ufg.infrastructure.adapter.network

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
        println("C++ fired onLocalDescription")
        signalingListener?.onLocalDescription(sdp)
    }

    fun onLocalCandidate(
        candidate: String,
        mid: String,
    ) {
        println("C++ fired onLocalCandidate")
        signalingListener?.onLocalCandidate(candidate, mid)
    }

    fun onDataChannelOpen() {
        println("C++ fired onDataChannelOpen")
        dataChannelListener?.onDataChannelOpen()
    }

    fun onDataChannelClose() {
        println("C++ fired onDataChannelClose")
        dataChannelListener?.onDataChannelClose()
    }

    fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
    ) {
        dataChannelListener?.onRemoteInput(inputMask, frameNumber)
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

    external override fun close()
}
