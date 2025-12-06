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

@Suppress("Indentation")
class GameLogicSpec :
    FunSpec({

        val player1 =
            Player(
                id = 1,
                name = "muni",
                position = Position(200.0, 500.0),
                nextMove = Movement(Direction(1.0, 0.0), Position(0.0, 0.0), 20.0, speedY = 0.0),
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

        test("Player health should decrease each frame") {
            val newState = GameLogic.defaultGameLogic(initialState)

            newState.players[1]?.health?.current shouldBe player1.health.current - 25
            newState.players[2]?.health?.current shouldBe player2.health.current - 25
        }
    })
