package com.heyteam.ufg.infrastructure.adapter.network

interface DataChannelListener {
    fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    )

    fun onDataChannelOpen()

    fun onDataChannelClose()
}
