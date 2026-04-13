package com.heyteam.ufg.application.port

import com.heyteam.ufg.application.port.input.NetworkInputPort
import com.heyteam.ufg.application.port.output.NetworkOutputPort

interface NetworkPort :
    NetworkInputPort,
    NetworkOutputPort {
    fun isConnected(): Boolean
}
