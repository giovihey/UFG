package com.heyteam.ufg.application.port.input

import com.heyteam.ufg.domain.component.InputState

interface NetworkInputPort {
    // Legacy delay-based API: returns and removes a single frame's input if already received.
    // Retained for compatibility; rollback code uses drainRemoteInputs() instead.
    fun pollRemoteInput(frameNumber: Long): InputState?

    // Non-blocking: return every authoritative remote input packet received since the last drain.
    // Order is unspecified; callers must key by frame number.
    fun drainRemoteInputs(): List<FramedInput>

    // Highest local sim frame the peer has reported (via the senderCurrentFrame piggyback on
    // every input packet). Used by the rollback service for time synchronization: if we are
    // running too far ahead of the peer, we stall a tick to let them catch up.
    // Returns -1 until we have received the first packet.
    fun peerFrame(): Long = -1L

    /**
     * Drain every (committedFrame, committedHash) pair the peer has reported since the last
     * call. Used by the rollback service to detect cross-machine desyncs by comparing the
     * peer's committed-frame hash against our own. Empty list = no new pairs.
     */
    fun drainRemoteCommittedHashes(): List<RemoteCommittedHash> = emptyList()
}

/** A `(frame, hash)` pair the peer has reported as committed (outside its rollback window). */
data class RemoteCommittedHash(
    val frame: Long,
    val hash: Long,
)
