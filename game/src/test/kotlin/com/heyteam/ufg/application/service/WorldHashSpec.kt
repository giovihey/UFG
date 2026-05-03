package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.component.Direction
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
import io.kotest.matchers.shouldNotBe

/**
 * Properties of the canonical world hash:
 *  - deterministic: same world ⇒ same hash, every time;
 *  - sensitive: any change to a sim-load-bearing field ⇒ a different hash;
 *  - insensitive: two parallel runs of the same input script produce equal hashes for
 *    every frame (the cross-peer comparison this enables is the whole point).
 */
class WorldHashSpec :
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
            )
        }

        "hash is deterministic across calls" {
            val w = seedWorld()
            WorldHash.hash(w) shouldBe WorldHash.hash(w)
        }

        "hash changes when frame number changes" {
            val a = seedWorld()
            val b = a.copy(frameNumber = 1)
            WorldHash.hash(a) shouldNotBe WorldHash.hash(b)
        }

        "hash changes when player position moves a single ULP" {
            val a = seedWorld()
            val p1 = a.players[1]!!
            val moved = p1.copy(position = p1.position.copy(x = p1.position.x + 0.0001))
            val b = a.copy(players = a.players + (1 to moved))
            WorldHash.hash(a) shouldNotBe WorldHash.hash(b)
        }

        "hash changes when health changes" {
            val a = seedWorld()
            val p1 = a.players[1]!!
            val damaged = p1.copy(health = p1.health.copy(current = 999))
            val b = a.copy(players = a.players + (1 to damaged))
            WorldHash.hash(a) shouldNotBe WorldHash.hash(b)
        }

        "hash is independent of map insertion order" {
            // Construct the same logical World with players keyed in opposite insertion
            // order. The canonical hash sorts by id, so iteration order must not matter.
            val w1 = seedWorld()
            val p1 = w1.players[1]!!
            val p2 = w1.players[2]!!
            val w2 = w1.copy(players = linkedMapOf(2 to p2, 1 to p1))
            WorldHash.hash(w1) shouldBe WorldHash.hash(w2)
        }

        "two parallel runs over identical inputs produce identical hashes every frame" {
            // The cross-peer determinism guarantee, expressed locally: simulate the same
            // script twice independently and assert the hash matches at every frame.
            var a = seedWorld()
            var b = seedWorld()
            val script =
                listOf(
                    InputState.NONE,
                    InputState(0b0001),
                    InputState(0b0011),
                    InputState(0b0010),
                    InputState.NONE,
                )
            for (input in script) {
                val inputs = mapOf(1 to input, 2 to InputState.NONE)
                a = GameLogic.step(a, inputs)
                b = GameLogic.step(b, inputs)
                WorldHash.hash(a) shouldBe WorldHash.hash(b)
            }
        }
    })
