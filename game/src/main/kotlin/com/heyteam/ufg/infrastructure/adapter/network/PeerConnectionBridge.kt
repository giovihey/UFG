package com.heyteam.ufg.infrastructure.adapter.network

interface PeerConnectionBridge {
    fun initialize(stunServer: String)

    fun createOffer(): String

    fun setRemoteDescription(sdp: String)

    fun addIceCandidate(
        candidate: String,
        mid: String,
    )

    fun sendInput(
        inputMask: Int,
        frameNumber: Long,
    )
}
