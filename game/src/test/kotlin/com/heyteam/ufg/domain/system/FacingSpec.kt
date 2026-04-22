package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Facing
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
import com.heyteam.ufg.infrastructure.adapter.output.JsonCharacterRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FacingSpec :
    StringSpec({

        val characters = JsonCharacterRepository()
        val ryo = characters.findById(1)!!

        fun player(facing: Facing): Player =
            Player(
                id = 1,
                name = "P1",
                position = Position(200.0, GameConstants.FLOOR_Y),
                nextMove = Movement(Direction(0.0, 0.0), Position(200.0, GameConstants.FLOOR_Y), 0.0, 0.0),
                health = Health(100, 100),
                hurtBox = Rectangle(0.0, 0.0, 50.0, 80.0),
                character = ryo,
                physicsState = PlayerPhysicsState(facing = facing),
            )

        fun world(p: Player) = World(frameNumber = 0, players = mapOf(1 to p), gameStatus = GameStatus.RUNNING)

        fun inputOf(vararg buttons: GameButton): InputState {
            var mask = 0
            buttons.forEach { mask = mask or it.bit }
            return InputState(mask)
        }

        "LEFT press updates facing to LEFT" {
            val r = InputSystem.apply(world(player(Facing.RIGHT)), mapOf(1 to inputOf(GameButton.LEFT)))
            r.players[1]!!.physicsState.facing shouldBe Facing.LEFT
        }

        "RIGHT press updates facing to RIGHT" {
            val r = InputSystem.apply(world(player(Facing.LEFT)), mapOf(1 to inputOf(GameButton.RIGHT)))
            r.players[1]!!.physicsState.facing shouldBe Facing.RIGHT
        }

        "pressing LEFT and RIGHT together preserves facing (walk back)" {
            val r =
                InputSystem.apply(
                    world(player(Facing.RIGHT)),
                    mapOf(1 to inputOf(GameButton.LEFT, GameButton.RIGHT)),
                )
            r.players[1]!!.physicsState.facing shouldBe Facing.RIGHT
        }

        "no horizontal input preserves facing" {
            val r = InputSystem.apply(world(player(Facing.LEFT)), mapOf(1 to InputState.NONE))
            r.players[1]!!.physicsState.facing shouldBe Facing.LEFT
        }

        "hitbox mirrors when facing LEFT" {
            val move = ryo.moveList.values.first()
            val active = AttackState(attack = move, currentFrame = move.startupFrames)
            val facingRight = player(Facing.RIGHT).copy(attackState = active)
            val facingLeft =
                facingRight.copy(
                    physicsState = facingRight.physicsState.copy(facing = Facing.LEFT),
                )
            val rightBox = facingRight.activeHitBox!!
            val leftBox = facingLeft.activeHitBox!!
            val rightCenter = facingRight.topLeft.x + facingRight.effectiveHurtbox.width / 2
            val leftCenter = facingLeft.topLeft.x + facingLeft.effectiveHurtbox.width / 2
            (leftBox.x < leftCenter) shouldBe true
            (rightBox.x >= rightCenter) shouldBe true
        }
    })
