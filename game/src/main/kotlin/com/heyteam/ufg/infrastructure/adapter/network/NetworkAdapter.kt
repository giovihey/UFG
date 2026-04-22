package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.domain.component.InputState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

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

    @Volatile private var connected = false

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
    ) {
        bridge.sendInput(inputState.mask, frameNumber)
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

    // Called from C++ via JNI when the remote player's input arrives
    override fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
    ) {
        val input = InputState(inputMask)
        // Legacy path (delay-based): keep populating the per-frame map so any caller still
        // using pollRemoteInput() continues to work.
        receivedInputs.put(frameNumber, input)
        // Rollback path: enqueue for non-blocking drain. Duplicates (from redundant sends)
        // are fine — the rollback service dedupes on insertion.
        inboundQueue.add(FramedInput(frameNumber, input))
    }

    override fun isConnected(): Boolean = connected

    override fun onDataChannelOpen() {
        connected = true
    }

    override fun onDataChannelClose() {
        println("Peer disconnected. Closing game.")
        connected = false
    }

    override fun close() {
        bridge.close()
    }
}
