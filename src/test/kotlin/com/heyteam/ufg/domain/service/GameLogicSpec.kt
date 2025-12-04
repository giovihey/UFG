package com.heyteam.ufg.domain.service

import com.heyteam.ufg.domain.model.Character
import com.heyteam.ufg.domain.model.Direction
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.GameStatus
import com.heyteam.ufg.domain.model.Health
import com.heyteam.ufg.domain.model.Movement
import com.heyteam.ufg.domain.model.Player
import com.heyteam.ufg.domain.model.Position
import com.heyteam.ufg.domain.physics.Rectangle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@Suppress("Indentation")
class GameLogicSpec :
    FunSpec({

        val player1 =
            Player(
                id = 1,
                name = "muni",
                position = Position(200.0, 500.0),
                nextMove = Movement(Direction(1.0, 0.0), Position(0.0, 0.0), 200),
                health = Health(100, 100),
                hurtBox = Rectangle(200.0, 500.0, 60.0, 120.0),
                character =
                    Character(
                        id = 1,
                        name = "Ryu",
                        maxHealth = Health(100, 100),
                        moveList = emptyMap(),
                        defaultHurtbox = Rectangle(0.0, 0.0, 60.0, 120.0),
                    ),
            )

        val player2 =
            player1.copy(
                id = 2,
                name = "giovi",
                position = Position(700.0, 500.0),
                hurtBox = Rectangle(700.0, 500.0, 60.0, 120.0),
                character =
                    Character(
                        id = 1,
                        name = "Ken",
                        maxHealth = Health(100, 100),
                        moveList = emptyMap(),
                        defaultHurtbox = Rectangle(0.0, 0.0, 60.0, 120.0),
                    ),
            )

        val initialState =
            GameState(
                frameNumber = 0L,
                players = mapOf(1 to player1, 2 to player2),
                gameStatus = GameStatus.RUNNING,
            )

        test("Player position should update each frame") {
            val deltaTime = 0.016 // ~60 FPS
            val newState = GameLogic.defaultGameLogic(initialState, deltaTime)

            newState.players[1]?.position?.x shouldNotBe player1.position.x
            newState.players[1]?.position?.x shouldBe initialState.players[1]?.position?.x!! + 100 * deltaTime
        }

        test("Player health should decrease each frame") {
            val deltaTime = 0.016
            val newState = GameLogic.defaultGameLogic(initialState, deltaTime)

            newState.players[1]?.health?.current shouldBe player1.health.current - 25
            newState.players[2]?.health?.current shouldBe player2.health.current - 25
        }

        test("Frame number should increment") {
            val deltaTime = 0.016
            val newState = GameLogic.defaultGameLogic(initialState, deltaTime)

            newState.frameNumber shouldBe initialState.frameNumber + 1
        }

        test("Player should not move beyond stage width") {
            val player = player1.copy(position = Position(950.0, 500.0)) // Near edge
            val stateNearEdge = initialState.copy(players = mapOf(1 to player, 2 to player2))

            val newState = GameLogic.defaultGameLogic(stateNearEdge, 1.0) // Large delta

            newState.players[1]?.position?.x shouldBe (950.0 + 100 * 1.0).coerceIn(0.0, 900.0)
        }

        test("Game should not update when status is not RUNNING") {
            val pausedState = initialState.copy(gameStatus = GameStatus.PAUSED)
            val newState = GameLogic.defaultGameLogic(pausedState, 0.016)

            newState shouldBe pausedState // No changes
        }

        test("Hurt box should follow position") {
            val deltaTime = 0.016
            val newState = GameLogic.defaultGameLogic(initialState, deltaTime)

            val updatedPlayer = newState.players[1]!!
            updatedPlayer.hurtBox.x shouldBe updatedPlayer.position.x
            updatedPlayer.hurtBox.y shouldBe updatedPlayer.position.y
        }
    })
