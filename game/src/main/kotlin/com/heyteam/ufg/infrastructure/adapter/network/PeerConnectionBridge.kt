package com.heyteam.ufg.infrastructure.adapter.network

interface PeerConnectionBridge {
    fun initialize(stunServer: String)

    fun createOffer(): String

    fun setRemoteDescription(sdp: String)

    fun addIceCandidate(
        candidate: String,
        mid: String,
    )

    /**
     * Wire format (36 bytes, little-endian):
     *
     *   off  size  field
     *   0    4     inputMask           (int32)
     *   4    8     frameNumber         (int64)
     *   12   8     senderCurrentFrame  (int64) — for time-sync stalling
     *   20   8     committedFrame      (int64) — Long.MIN_VALUE = no hash piggyback
     *   28   8     committedHash       (int64) — canonical WorldHash; ignored when
     *                                            committedFrame is Long.MIN_VALUE
     */
    fun sendInput(
        inputMask: Int,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    )

    fun close()
}
