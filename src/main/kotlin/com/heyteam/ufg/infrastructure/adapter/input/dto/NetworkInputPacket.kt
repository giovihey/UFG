package com.heyteam.ufg.infrastructure.adapter.input.dto

data class NetworkInputPacket(
    val playerId: Int,
    val frameNumber: Long,
    val inputMask: Int,
)
