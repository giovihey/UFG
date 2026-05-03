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

            adapter.onRemoteInput(
                inputMask = 0b0101,
                frameNumber = 42L,
                senderCurrentFrame = 40L,
                committedFrame = Long.MIN_VALUE,
                committedHash = 0L,
            )

            adapter.pollRemoteInput(42L) shouldBe InputState(0b0101)
        }

        test("pollRemoteInput returns NONE for missing frame") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.pollRemoteInput(99L) shouldBe null
        }

        test("sendInput delegates to bridge") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.sendInput(InputState(0b0011), 10L, 12L, 5L, 0xDEADBEEFL)

            verify { bridge.sendInput(0b0011, 10L, 12L, 5L, 0xDEADBEEFL) }
        }

        test("peerFrame tracks the highest senderCurrentFrame seen") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            adapter.peerFrame() shouldBe -1L
            adapter.onRemoteInput(0, 5L, 5L, Long.MIN_VALUE, 0L)
            adapter.peerFrame() shouldBe 5L
            adapter.onRemoteInput(0, 7L, 7L, Long.MIN_VALUE, 0L)
            adapter.peerFrame() shouldBe 7L
            // Out-of-order packet: peerFrame must not regress.
            adapter.onRemoteInput(0, 6L, 6L, Long.MIN_VALUE, 0L)
            adapter.peerFrame() shouldBe 7L
        }

        test("drainRemoteCommittedHashes dedupes by frame and ignores sentinel") {
            val bridge = mockk<PeerConnectionBridge>(relaxed = true)
            val adapter = NetworkAdapter(bridge)

            // Sentinel = ignored.
            adapter.onRemoteInput(0, 1L, 1L, Long.MIN_VALUE, 0L)
            // Two distinct frames -> two entries.
            adapter.onRemoteInput(0, 2L, 2L, 100L, 0xAAAAL)
            adapter.onRemoteInput(0, 3L, 3L, 101L, 0xBBBBL)
            // Duplicate of frame 100 (from redundant send window) -> dropped.
            adapter.onRemoteInput(0, 4L, 4L, 100L, 0xAAAAL)

            val drained = adapter.drainRemoteCommittedHashes()
            drained.map { it.frame } shouldBe listOf(100L, 101L)
        }
    })
