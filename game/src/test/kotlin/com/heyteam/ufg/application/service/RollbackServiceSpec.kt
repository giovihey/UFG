package com.heyteam.ufg.application.service

import com.heyteam.ufg.application.port.input.FramedInput
import com.heyteam.ufg.application.port.input.NetworkInputPort
import com.heyteam.ufg.application.port.output.NetworkOutputPort
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import com.heyteam.ufg.domain.system.GameLogic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Correctness property: regardless of how much the authoritative remote input is delayed
 * (up to MAX_ROLLBACK_FRAMES), the final World produced by RollbackService must equal the
 * World produced by a zero-latency reference run over the same input script.
 */
class RollbackServiceSpec :
    StringSpec({

        fun seedWorld(): World {
            fun p(
                id: Int,
                x: Double,
            ) = Player(
                id = id,
                name = "P$id",
                position = Position(x, GameConstants.FLOOR_Y),
                nextMove = Movement(Direction(0.0, 0.0), Position(x, GameConstants.FLOOR_Y), 0.0, 0.0),
                health = Health(current = 1000, max = 1000),
                hurtBox = Rectangle(x, GameConstants.FLOOR_Y, 50.0, 80.0),
                physicsState = PlayerPhysicsState(),
            )
            return World(
                frameNumber = 0,
                players = mapOf(1 to p(1, 200.0), 2 to p(2, 500.0)),
                gameStatus = GameStatus.RUNNING,
                roundTimer = 99,
            )
        }

        fun scriptedInputs(
            frames: Int,
            seed: Long = 0xABCDEFL,
        ): List<Pair<InputState, InputState>> {
            var state = seed

            fun next(): Int {
                state = (state * 1103515245L + 12345L) and 0x7fffffffL
                return state.toInt()
            }
            val buttons = GameButton.values()
            return (0 until frames).map {
                val m1 = buttons.fold(0) { acc, b -> if ((next() and 7) == 0) acc or b.bit else acc }
                val m2 = buttons.fold(0) { acc, b -> if ((next() and 7) == 0) acc or b.bit else acc }
                InputState(m1) to InputState(m2)
            }
        }

        // Reference: zero-latency run. Both sides apply INPUT_DELAY symmetrically, so on
        // game-frame F the world sees player inputs sampled at wall-frame (F - inputDelay).
        fun referenceRun(
            script: List<Pair<InputState, InputState>>,
            inputDelay: Int,
            totalFrames: Int,
        ): World {
            var world = seedWorld()
            for (frame in 0 until totalFrames) {
                val a = script.getOrNull(frame - inputDelay)?.first ?: InputState.NONE
                val b = script.getOrNull(frame - inputDelay)?.second ?: InputState.NONE
                world = GameLogic.step(world, mapOf(1 to a, 2 to b))
            }
            return world
        }

        /**
         * Simulated peer. The peer is running an identical RollbackService on its side, so
         * the authoritative input it sends for gameplay-frame F is what it sampled at its
         * local wall-frame (F - inputDelay). That pre-scheduled input is then delivered to
         * us [networkDelay] wall-frames later.
         */
        class DelayedRemote(
            private val remoteScript: List<InputState>,
            private val inputDelay: Int,
            private val networkDelay: Int,
        ) : NetworkInputPort,
            NetworkOutputPort {
            private var tickCount = 0
            private val pending = ArrayDeque<Pair<Long, FramedInput>>()

            override fun pollRemoteInput(frameNumber: Long): InputState? = null

            override fun drainRemoteInputs(): List<FramedInput> {
                val out = ArrayList<FramedInput>()
                while (pending.isNotEmpty() && pending.first().first <= tickCount.toLong()) {
                    out.add(pending.removeFirst().second)
                }
                return out
            }

            override fun sendInput(
                inputState: InputState,
                frameNumber: Long,
            ) { /* local echo — not used by this fake */ }

            fun advance() {
                val wallFrame = tickCount.toLong()
                val idx = wallFrame.toInt()
                if (idx in remoteScript.indices) {
                    // Peer schedules its raw input at game-frame = wallFrame + inputDelay,
                    // then the packet takes networkDelay wall-frames to reach us.
                    val gameFrame = wallFrame + inputDelay
                    val arrivalTick = wallFrame + networkDelay
                    pending.addLast(arrivalTick to FramedInput(gameFrame, remoteScript[idx]))
                }
                tickCount++
            }
        }

        fun rollbackRun(
            script: List<Pair<InputState, InputState>>,
            networkDelay: Int,
            inputDelay: Int,
            totalFrames: Int,
        ): World {
            val engine = GameEngine(seedWorld())
            val transport = DelayedRemote(script.map { it.second }, inputDelay, networkDelay)
            val service =
                RollbackService(
                    engine = engine,
                    networkInput = transport,
                    networkOutput = transport,
                    localPlayerId = 1,
                    remotePlayerId = 2,
                    config = RollbackConfig(inputDelay = inputDelay),
                )

            for (frame in 0 until totalFrames) {
                val localInput = script.getOrNull(frame)?.first ?: InputState.NONE
                transport.advance()
                service.tick(localInput)
            }
            return engine.getWorld()
        }

        "zero-delay rollback matches reference run" {
            val script = scriptedInputs(120)
            val inputDelay = 2
            val total = script.size + inputDelay
            val ref = referenceRun(script, inputDelay, total)
            val rb = rollbackRun(script, networkDelay = 0, inputDelay = inputDelay, totalFrames = total)
            rb.frameNumber shouldBe ref.frameNumber
            rb.players[1]!!.position shouldBe ref.players[1]!!.position
            rb.players[2]!!.position shouldBe ref.players[2]!!.position
            rb.players[1]!!.health shouldBe ref.players[1]!!.health
            rb.players[2]!!.health shouldBe ref.players[2]!!.health
        }

        "delayed authoritative input still converges to reference state" {
            val script = scriptedInputs(120, seed = 0x9E3779B1L)
            val inputDelay = 2
            // Sweep network delays within the rollback window.
            listOf(1, 2, 4, 6).forEach { networkDelay ->
                // Total frames must be large enough that every authoritative remote packet
                // has been delivered by the final tick — otherwise the last few frames are
                // still running on predictions and can legitimately differ from reference.
                val total = script.size + inputDelay + networkDelay + 2
                val ref = referenceRun(script, inputDelay, total)
                val rb =
                    rollbackRun(
                        script,
                        networkDelay = networkDelay,
                        inputDelay = inputDelay,
                        totalFrames = total,
                    )
                rb.players[1]!!.position shouldBe ref.players[1]!!.position
                rb.players[2]!!.position shouldBe ref.players[2]!!.position
                rb.players[1]!!.health shouldBe ref.players[1]!!.health
                rb.players[2]!!.health shouldBe ref.players[2]!!.health
            }
        }
    })
