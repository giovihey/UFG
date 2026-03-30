package com.heyteam.ufg.infrastructure.adapter.network

interface SignalingListener {
    fun onLocalDescription(sdp: String)

    fun onLocalCandidate(
        candidate: String,
        mid: String,
    )
}
