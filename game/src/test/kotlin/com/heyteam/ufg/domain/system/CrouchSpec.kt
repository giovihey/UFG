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
import com.heyteam.ufg.infrastructure.adapter.output.JsonCharacterRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CrouchSpec :
    StringSpec({

        val characters = JsonCharacterRepository()
        val allRounder = characters.findById(1)!!

        fun groundedPlayer(): Player =
            Player(
                id = 1,
                name = "P1",
                position = Position(200.0, GameConstants.FLOOR_Y),
                nextMove = Movement(Direction(0.0, 0.0), Position(200.0, GameConstants.FLOOR_Y), 0.0, 0.0),
                health = Health(100, 100),
                hurtBox = Rectangle(0.0, 0.0, 50.0, 80.0),
                character = allRounder,
            )

        fun world(player: Player) =
            World(
                frameNumber = 0,
                players = mapOf(1 to player),
                gameStatus = GameStatus.RUNNING,
            )

        fun inputOf(vararg buttons: GameButton): InputState {
            var mask = 0
            buttons.forEach { mask = mask or it.bit }
            return InputState(mask)
        }

        "DOWN while grounded sets isCrouching" {
            val result = InputSystem.apply(world(groundedPlayer()), mapOf(1 to inputOf(GameButton.DOWN)))
            result.players[1]!!.physicsState.isCrouching shouldBe true
        }

        "effectiveHurtbox halves when crouching" {
            val result = InputSystem.apply(world(groundedPlayer()), mapOf(1 to inputOf(GameButton.DOWN)))
            val p = result.players[1]!!
            p.effectiveHurtbox.height shouldBe p.hurtBox.height / 2
        }

        "releasing DOWN clears isCrouching" {
            val down = mapOf(1 to inputOf(GameButton.DOWN))
            val crouched = InputSystem.apply(world(groundedPlayer()), down)
            val released = InputSystem.apply(crouched, mapOf(1 to InputState.NONE))
            released.players[1]!!.physicsState.isCrouching shouldBe false
        }

        "DOWN + PUNCH selects the low move" {
            val inputs = mapOf(1 to inputOf(GameButton.DOWN, GameButton.PUNCH))
            val result = InputSystem.apply(world(groundedPlayer()), inputs)
            result.players[1]!!
                .attackState!!
                .attack.name shouldBe "Low Jab"
        }

        "holding DOWN keeps isCrouching while attacking" {
            val inputs = mapOf(1 to inputOf(GameButton.DOWN, GameButton.PUNCH))
            val result = InputSystem.apply(world(groundedPlayer()), inputs)
            result.players[1]!!.physicsState.isCrouching shouldBe true
        }

        "LEFT + KICK falls back to NEUTRAL_KICK when no LEFT_KICK defined" {
            val inputs = mapOf(1 to inputOf(GameButton.LEFT, GameButton.KICK))
            val result = InputSystem.apply(world(groundedPlayer()), inputs)
            result.players[1]!!
                .attackState!!
                .attack.name shouldBe "Roundhouse"
        }

        "DOWN cannot walk (direction.x forced to 0)" {
            val result =
                InputSystem.apply(
                    world(groundedPlayer()),
                    mapOf(1 to inputOf(GameButton.DOWN, GameButton.LEFT)),
                )
            result.players[1]!!
                .nextMove.direction.x shouldBe 0.0
        }
    })
