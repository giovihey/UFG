package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.input.NetworkInputPort
import com.heyteam.ufg.application.port.output.NetworkOutputPort
import com.heyteam.ufg.domain.component.InputState
import java.util.concurrent.ConcurrentLinkedQueue

class NetworkAdapter(
    private val bridge: WebRtcBridge
) : NetworkOutputPort, NetworkInputPort {
//    Thread 1: libdatachannel's internal thread
//    → receives bytes from remote player
//    → calls C++ onMessage callback
//    → calls JNI → onRemoteInput()
//    → WRITES to the queue
//
//    Thread 2: your game loop thread
//    → calls pollRemoteInput()
//    → READS from the queue
//    private val receivedInputs = ConcurrentLinkedQueue<Long, InputState>()
    override fun sendInput(inputState: InputState, frameNumber: Long) {
        bridge.sendInput(inputState.mask, frameNumber)
    }

    override fun pollRemoteInput(frameNumber: Long): InputState? {
//        // Find and remove the input for this frame
//        val iterator = receivedInputs.iterator()
//        while (iterator.hasNext()) {
//            val (frame, input) = iterator.next()
//            if (frame == frameNumber) {
//                iterator.remove()
//                return input
//            }
//        }
        return null  // not arrived yet
    }

    // Called from C++ via JNI when the remote player's input arrives
//    fun onRemoteInput(inputMask: Int, frameNumber: Long) {
//        receivedInputs.add(frameNumber to InputState(inputMask))
//    }

}