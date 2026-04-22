package com.heyteam.ufg.application.service

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
 * Simulation must be bit-identical when fed the same inputs. Rollback depends on this
 * property: if replaying the same (world, input) sequence doesn't produce the same world,
 * the rewind-and-replay will "correct" into divergence.
 */
class DeterminismSpec :
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

        // Deterministic pseudo-random input script. We don't use java.util.Random here
        // because the *test* needs to produce the same script on every run — a fixed LCG
        // with a constant seed does that across JVMs.
        fun scriptedInputs(frames: Int): List<Pair<InputState, InputState>> {
            var state = 0x12345678L

            fun next(): Int {
                state = (state * 1103515245L + 12345L) and 0x7fffffffL
                return state.toInt()
            }
            val buttons = GameButton.values()
            return (0 until frames).map {
                val m1 = buttons.fold(0) { acc, b -> if ((next() and 3) == 0) acc or b.bit else acc }
                val m2 = buttons.fold(0) { acc, b -> if ((next() and 3) == 0) acc or b.bit else acc }
                InputState(m1) to InputState(m2)
            }
        }

        fun run(script: List<Pair<InputState, InputState>>): List<Int> {
            var world = seedWorld()
            val hashes = ArrayList<Int>(script.size)
            for ((a, b) in script) {
                world = GameLogic.step(world, mapOf(1 to a, 2 to b))
                hashes.add(world.hashCode())
            }
            return hashes
        }

        "GameLogic.step is bit-identical across two runs of the same script" {
            val script = scriptedInputs(600)
            run(script) shouldBe run(script)
        }

        "Long scripted run is stable (regression guard)" {
            val script = scriptedInputs(3600)
            val a = run(script)
            val b = run(script)
            a.last() shouldBe b.last()
            a.size shouldBe 3600
        }
    })
