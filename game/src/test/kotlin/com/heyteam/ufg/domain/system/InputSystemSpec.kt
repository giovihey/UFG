package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.InputState
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InputSystemSpec :
    StringSpec({

        // ── Helper: create a player with sensible defaults ──────────────────
        fun createPlayer(
            posX: Double = 200.0,
            posY: Double = GameConstants.FLOOR_Y,
            speedY: Double = 0.0,
        ): Player =
            Player(
                id = 1,
                name = "P1",
                position = Position(posX, posY),
                nextMove =
                    Movement(
                        direction = Direction(0.0, 0.0),
                        position = Position(posX, posY),
                        speedX = 0.0,
                        speedY = speedY,
                    ),
                health = Health(100, 100),
                hurtBox = Rectangle(posX, posY, 50.0, 80.0),
            )

        // ── Helper: build a world with one player ───────────────────────────
        fun worldWith(player: Player): World =
            World(
                frameNumber = 0,
                players = mapOf(1 to player),
                gameStatus = GameStatus.RUNNING,
            )

        // ── Helper: build an InputState from buttons ────────────────────────
        fun inputOf(vararg buttons: GameButton): InputState {
            var mask = 0
            buttons.forEach { mask = mask or it.bit }
            return InputState(mask)
        }

        // ── Helper: apply input and return the updated player ───────────────
        fun applyAndGetPlayer(
            player: Player,
            vararg buttons: GameButton,
        ): Player {
            val result = InputSystem.apply(worldWith(player), mapOf(1 to inputOf(*buttons)))
            return result.players[1]!!
        }

        // ========== NO INPUT ==========
        "no input: direction stays (0, 0)" {
            val updated = applyAndGetPlayer(createPlayer())
            updated.nextMove.direction shouldBe Direction(0.0, 0.0)
        }

        "no input: speedY unchanged" {
            val updated = applyAndGetPlayer(createPlayer())
            updated.nextMove.speedY shouldBe 0.0
        }

        // ========== HORIZONTAL MOVEMENT ==========
        "LEFT sets direction.x to -1" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.LEFT)
            updated.nextMove.direction.x shouldBe -1.0
        }

        "RIGHT sets direction.x to 1" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.RIGHT)
            updated.nextMove.direction.x shouldBe 1.0
        }

        "LEFT + RIGHT: LEFT takes priority (first in when branch)" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.LEFT, GameButton.RIGHT)
            updated.nextMove.direction.x shouldBe -1.0
        }

        "horizontal input does not affect speedY" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.RIGHT)
            updated.nextMove.speedY shouldBe 0.0
        }

        // ========== JUMP ==========
        "JUMP while grounded sets speedY to JUMP_INITIAL_VELOCITY" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.JUMP)
            updated.nextMove.speedY shouldBe GameConstants.JUMP_INITIAL_VELOCITY
        }

        "JUMP while airborne does NOT reset speedY (no double jump)" {
            val airbornePlayer = createPlayer(posY = 100.0, speedY = 50.0)
            val updated = applyAndGetPlayer(airbornePlayer, GameButton.JUMP)
            updated.nextMove.speedY shouldBe 50.0
        }

        "JUMP does not affect horizontal direction" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.JUMP)
            updated.nextMove.direction.x shouldBe 0.0
        }

        // ========== JUMP + HORIZONTAL (diagonal jump) ==========
        "JUMP + RIGHT: both apply independently" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.JUMP, GameButton.RIGHT)
            updated.nextMove.direction.x shouldBe 1.0
            updated.nextMove.speedY shouldBe GameConstants.JUMP_INITIAL_VELOCITY
        }

        "JUMP + LEFT: both apply independently" {
            val updated = applyAndGetPlayer(createPlayer(), GameButton.JUMP, GameButton.LEFT)
            updated.nextMove.direction.x shouldBe -1.0
            updated.nextMove.speedY shouldBe GameConstants.JUMP_INITIAL_VELOCITY
        }

        // ========== MISSING PLAYER ==========
        "returns world unchanged if no players exist" {
            val emptyWorld =
                World(
                    frameNumber = 0,
                    players = emptyMap(),
                    gameStatus = GameStatus.RUNNING,
                )
            val result = InputSystem.apply(emptyWorld, mapOf(1 to inputOf(GameButton.RIGHT)))
            result shouldBe emptyWorld
        }

        // ========== POSITION UNCHANGED ==========
        "input never modifies player position directly" {
            val player = createPlayer(posX = 200.0)
            val updated = applyAndGetPlayer(player, GameButton.RIGHT, GameButton.JUMP)
            updated.position shouldBe player.position
        }
    })
