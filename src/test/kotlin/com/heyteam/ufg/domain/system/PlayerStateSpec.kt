package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.PlayerState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class PlayerStateSpec :
    StringSpec({

        fun createPlayer(
            direction: Double = 0.0,
            posX: Double = 100.0,
            posY: Double = GameConstants.FLOOR_Y,
            speedY: Double = 0.0,
        ): Player =
            Player(
                id = 1,
                name = "Test",
                position = Position(posX, posY),
                nextMove =
                    Movement(
                        direction = Direction(direction, 0.0),
                        position = Position(posX, posY),
                        speedX = 0.0,
                        speedY = speedY,
                    ),
                health = Health(100, 100),
                hurtBox = Rectangle(posX, posY, 50.0, 80.0),
                physicsState = PlayerPhysicsState(),
            )

        fun updatePlayer(
            player: Player,
            dt: Double = 0.01667,
        ): Player {
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )
            return PhysicsSystem
                .update(world, dt)
                .players[1]!!
        }

        fun updateWorld(
            world: World,
            dt: Double = 0.01667,
        ): World = PhysicsSystem.update(world, dt)

        "IDLE: no direction + grounded = IDLE" {
            val player = createPlayer(direction = 0.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.physicsState.state shouldBe PlayerState.IDLE
        }

        "IDLE: position unchanged when no input" {
            val player = createPlayer(direction = 0.0, posX = 100.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.position.x shouldBe 100.0
            updated.position.y shouldBe GameConstants.FLOOR_Y
        }

        // ========== WALKING STATE ==========
        "WALKING: when direction is positive, state changes to WALKING" {
            val player = createPlayer(direction = 1.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.physicsState.state shouldBe PlayerState.WALKING
        }

        "WALKING: position X increases when direction is positive" {
            val player = createPlayer(direction = 1.0, posX = 100.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.position.x shouldBeGreaterThan 100.0
        }

        "WALKING: when direction is negative, state changes to WALKING" {
            val player = createPlayer(direction = -1.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.physicsState.state shouldBe PlayerState.WALKING
        }

        "WALKING: position X decreases when direction is negative" {
            val player = createPlayer(direction = -1.0, posX = 150.0, posY = GameConstants.FLOOR_Y)
            val updated = updatePlayer(player)
            updated.position.x shouldBe 150.0 - (GameConstants.WALK_SPEED * 0.01667)
        }

        // ========== JUMPING STATE ==========
        "JUMPING: when player is above floor = JUMPING state" {
            val player = createPlayer(direction = 0.0, posY = 100.0, speedY = 0.0)
            val updated = updatePlayer(player)
            updated.physicsState.state shouldBe PlayerState.JUMPING
        }

        "JUMPING: gravity pulls player down" {
            val player = createPlayer(direction = 0.0, posY = 100.0, speedY = 0.0)
            val updated = updatePlayer(player)
            updated.position.y shouldBeGreaterThan 100.0
        }

        "JUMPING: speedY increases when falling (gravity applied)" {
            val player = createPlayer(direction = 0.0, posY = 100.0, speedY = 0.0)
            val updated = updatePlayer(player)
            updated.nextMove.speedY shouldBeGreaterThan 0.0
        }

        "FLOOR COLLISION: speedY resets to 0 when landing on floor" {
            var player = createPlayer(direction = 0.0, posY = 100.0, speedY = 0.0)
            var world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            // Simulate multiple frames until player reaches floor
            repeat(100) {
                val result = updateWorld(world)
                player = result.players[1]!!
                world = result
            }

            player.nextMove.speedY shouldBe 0.0
            player.position.y shouldBe GameConstants.FLOOR_Y
            player.physicsState.state shouldBe PlayerState.IDLE
        }
    })
