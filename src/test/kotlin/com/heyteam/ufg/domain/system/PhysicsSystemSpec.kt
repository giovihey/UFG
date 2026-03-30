package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

class PhysicsSystemSpec :
    StringSpec({

        fun createPlayer(
            id: Int,
            posX: Double = 100.0,
            posY: Double = GameConstants.FLOOR_Y,
            direction: Double = 0.0,
            speedY: Double = 0.0,
        ): Player =
            Player(
                id = id,
                name = "Player $id",
                position = Position(posX, posY),
                nextMove =
                    Movement(
                        direction = Direction(direction, 0.0),
                        position = Position(posX, posY),
                        speedX = 0.0,
                        speedY = speedY,
                    ),
                health = Health(current = 100, max = 100),
                hurtBox = Rectangle(posX, posY, 50.0, 80.0),
                physicsState = PlayerPhysicsState(),
            )

        fun updateWorld(
            world: World,
            dt: Double = 0.01667,
        ): World = PhysicsSystem.update(world, dt)

        // ========== SINGLE PLAYER PHYSICS ==========
        "World with single player updates correctly" {
            val player = createPlayer(id = 1, posX = 200.0, posY = GameConstants.FLOOR_Y)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)

            result.players.size shouldBe 1
            result.players[1] shouldBe player
        }

        "Player moves right with positive direction" {
            val player = createPlayer(id = 1, posX = 100.0, direction = 1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)
            val updatedPlayer = result.players[1]!!

            updatedPlayer.position.x shouldBeGreaterThan 100.0
        }

        "Player moves left with negative direction" {
            val player = createPlayer(id = 1, posX = 150.0, direction = -1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)
            val updatedPlayer = result.players[1]!!

            updatedPlayer.position.x shouldBeLessThan 150.0
        }

        "Gravity applied to player in air" {
            val player = createPlayer(id = 1, posY = 100.0, speedY = 0.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)
            val updatedPlayer = result.players[1]!!

            updatedPlayer.position.y shouldBeGreaterThan 100.0
            updatedPlayer.nextMove.speedY shouldBeGreaterThan 0.0
        }

        "Player falls towards floor and eventually reaches it" {
            var player = createPlayer(id = 1, posY = 100.0, speedY = 0.0)
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

            player.position.y shouldBe GameConstants.FLOOR_Y
            player.nextMove.speedY shouldBe 0.0
        }

        // ========== MULTIPLE PLAYERS ==========
        "World with multiple players updates all players" {
            val p1 = createPlayer(id = 1, posX = 100.0, direction = 1.0)
            val p2 = createPlayer(id = 2, posX = 400.0, direction = -1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to p1, 2 to p2),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)

            result.players.size shouldBe 2
            result.players[1]!!.position.x shouldBeGreaterThan 100.0
            result.players[2]!!.position.x shouldBeLessThan 400.0
        }

        // ========== GAME STATUS ==========
        "PhysicsSystem returns same world if game is not RUNNING" {
            val player = createPlayer(id = 1, direction = 1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.ROUND_END,
                )

            val result = PhysicsSystem.update(world, 0.01667)

            result shouldBe world
            result.players[1]!!.position.x shouldBe 100.0
        }

        // ========== BOUNDARY CHECKS ==========
        "Player cannot move left beyond STAGE_MARGIN" {
            val player = createPlayer(id = 1, posX = GameConstants.STAGE_MARGIN, direction = -1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)
            val updatedPlayer = result.players[1]!!

            updatedPlayer.topLeft.x shouldBe GameConstants.STAGE_MARGIN
        }

        "Player cannot move right beyond STAGE_WIDTH - STAGE_MARGIN" {
            val maxX = GameConstants.STAGE_WIDTH - GameConstants.STAGE_MARGIN
            val player = createPlayer(id = 1, posX = maxX, direction = 1.0)
            val world =
                World(
                    frameNumber = 0,
                    players = mapOf(1 to player),
                    gameStatus = GameStatus.RUNNING,
                )

            val result = updateWorld(world)
            val updatedPlayer = result.players[1]!!

            (updatedPlayer.topLeft.x + updatedPlayer.hurtBox.width) shouldBe maxX
        }
    })
