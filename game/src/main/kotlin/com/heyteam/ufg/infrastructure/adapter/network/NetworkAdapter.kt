package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.NetworkPort
import com.heyteam.ufg.domain.component.InputState
import java.util.concurrent.ConcurrentHashMap

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
    private var connected = false

    override fun sendInput(
        inputState: InputState,
        frameNumber: Long,
    ) {
        bridge.sendInput(inputState.mask, frameNumber)
    }

    override fun pollRemoteInput(frameNumber: Long): InputState? = receivedInputs.remove(frameNumber)

    // Called from C++ via JNI when the remote player's input arrives
    override fun onRemoteInput(
        inputMask: Int,
        frameNumber: Long,
    ) {
        println("Received remote input for frame $frameNumber")
        receivedInputs.put(frameNumber, InputState(inputMask))
    }

    fun isConnected(): Boolean = connected

    override fun onDataChannelOpen() {
        connected = true
    }
}
