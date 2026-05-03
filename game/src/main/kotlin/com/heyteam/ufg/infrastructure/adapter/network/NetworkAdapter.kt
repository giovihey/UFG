package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.application.port.input.RemoteCommittedHash
import com.heyteam.ufg.application.port.output.NetworkOutputPort
import com.heyteam.ufg.domain.component.InputState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger {}

class NetworkAdapter(
    private val bridge: PeerConnectionBridge,
) : NetworkPort,
    DataChannelListener {
//    Thread 1: libdatachannel's internal thread
//    → receives bytes from remote player
//    → calls C++ onMessage callback
//    → calls JNI → onRemoteInput()
//    → WRITES to the queue
//
//    Thread 2: your game loop thread
//    → calls pollRemoteInput()
//    → READS from the queue
    private val receivedInputs = ConcurrentHashMap<Long, InputState>()

    // Incoming queue for rollback: producer is the JNI callback thread, consumer is the
    // game loop calling drainRemoteInputs() once per tick. A bounded hand-off is enough;
    // duplicate frames (from the redundant send window) are deduped by the consumer.
    private val inboundQueue = ConcurrentLinkedQueue<FramedInput>()

    // Highest senderCurrentFrame the peer has ever reported, monotonically. Read on the
    // game-loop thread, written from the JNI callback thread, hence atomic.
    private val peerLatestSenderFrame = AtomicLong(-1L)

    // (frame, hash) pairs from the peer's committed-hash piggyback. We only enqueue the
    // *first* time we see a given frame to avoid flooding the consumer with duplicates from
    // the redundant send window.
    private val seenCommittedFrames = ConcurrentHashMap.newKeySet<Long>()
    private val inboundCommittedHashes = ConcurrentLinkedQueue<RemoteCommittedHash>()

    @Volatile private var connected = false

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    ) {
        bridge.sendInput(inputState.mask, frameNumber, senderCurrentFrame, committedFrame, committedHash)
    }

    override fun pollRemoteInput(frameNumber: Long): InputState? = receivedInputs.remove(frameNumber)

    override fun drainRemoteInputs(): List<FramedInput> {
        if (inboundQueue.isEmpty()) return emptyList()
        val drained = ArrayList<FramedInput>()
        while (true) {
            val next = inboundQueue.poll() ?: break
            drained.add(next)
        }
        return drained
    }

    override fun peerFrame(): Long = peerLatestSenderFrame.get()

    override fun drainRemoteCommittedHashes(): List<RemoteCommittedHash> {
        if (inboundCommittedHashes.isEmpty()) return emptyList()
        val drained = ArrayList<RemoteCommittedHash>()
        while (true) {
            val next = inboundCommittedHashes.poll() ?: break
            drained.add(next)
        }
        return drained
    }

    // Called from C++ via JNI when the remote player's input arrives
    override fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
        senderCurrentFrame: Long,
        committedFrame: Long,
        committedHash: Long,
    ) {
        val input = InputState(inputMask)
        // Legacy path (delay-based): keep populating the per-frame map so any caller still
        // using pollRemoteInput() continues to work.
        receivedInputs.put(frameNumber, input)
        // Rollback path: enqueue for non-blocking drain. Duplicates (from redundant sends)
        // are fine — the rollback service dedupes on insertion.
        inboundQueue.add(FramedInput(frameNumber, input))
        // Time-sync: track the highest peer sim frame we've seen. Monotonic — out-of-order
        // packets (older senderCurrentFrame) are ignored.
        peerLatestSenderFrame.updateAndGet { prev -> if (senderCurrentFrame > prev) senderCurrentFrame else prev }
        // Committed-hash piggyback: dedupe by frame so the redundant send window doesn't
        // flood the consumer.
        if (committedFrame != NetworkOutputPort.NO_COMMITTED_HASH && seenCommittedFrames.add(committedFrame)) {
            inboundCommittedHashes.add(RemoteCommittedHash(committedFrame, committedHash))
        }
    }

    override fun isConnected(): Boolean = connected

    override fun onDataChannelOpen() {
        connected = true
    }

    override fun onDataChannelClose() {
        log.warn { "Peer disconnected. Closing game." }
        connected = false
    }

    override fun close() {
        bridge.close()
    }
}
