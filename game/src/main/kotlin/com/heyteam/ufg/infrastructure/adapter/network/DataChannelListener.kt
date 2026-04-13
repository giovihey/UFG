package com.heyteam.ufg.infrastructure.adapter.network

interface DataChannelListener {
    fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
    )

    fun onDataChannelOpen()

    fun onDataChannelClose()
}
