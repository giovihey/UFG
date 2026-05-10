package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.application.port.input.RemoteCommittedHash
import com.heyteam.ufg.domain.component.InputState

/**
 * No-op [NetworkPort] for single-player practice mode.
 *
 * Contract:
 * - [isConnected] always returns true → [GameLoop] never stops on disconnection.
 * - [drainRemoteInputs] always returns empty → [RollbackService] predicts NONE for P2,
 *   so P2 stays idle every frame. No rollback ever fires.
 * - [peerFrame] returns -1 (the "no peer yet" sentinel) → [RollbackService] skips the
 *   time-sync stall path entirely (see the `peerFrame >= 0` guard in RollbackService.tick).
 * - All outbound sends are silently dropped.
 *
 * No changes to GameLoop, RollbackService, or any domain code are required.
 */
class LocalNetworkPort : NetworkPort {
    override fun isConnected(): Boolean = true

    override fun close() = Unit

    override fun pollRemoteInput(frameNumber: Long): InputState? = null

    override fun drainRemoteInputs(): List<FramedInput> = emptyList()

    /** -1 = no peer data yet → RollbackService skips its time-sync stall check. */
    override fun peerFrame(): Long = -1L

    override fun drainRemoteCommittedHashes(): List<RemoteCommittedHash> = emptyList()

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    ) = Unit
}
