package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.domain.component.InputState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class NetworkAdapterSpec :
    FunSpec({
        test("pollRemoteInput return input that was received") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.onRemoteInput(inputMask = 0b0101, frameNumber = 42L)

            adapter.pollRemoteInput(42L) shouldBe InputState(0b0101)
        }

        test("pollRemoteInput returns NONE for missing frame") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.pollRemoteInput(99L) shouldBe InputState.NONE
        }

        test("sendInput delegates to bridge") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.sendInput(InputState(0b0011), 10L)

            verify { bridge.sendInput(0b0011, 10L) }
        }
    })
