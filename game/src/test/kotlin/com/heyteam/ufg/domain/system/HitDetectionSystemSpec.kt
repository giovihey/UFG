package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Attack
import com.heyteam.ufg.domain.component.AttackState
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.GameStatus
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HitDetectionSystemSpec :
    StringSpec({

        val jab =
            Attack(
                name = "Jab",
                damage = 10,
                buttonInput = GameButton.PUNCH,
                hitBox = Rectangle(0.0, 0.0, 40.0, 30.0),
                startupFrames = 4,
                activeFrames = 3,
                recoveryFrames = 8,
                hitstunFrames = 12,
                knockbackSpeed = 150.0,
            )

        fun createPlayer(
            id: Int,
            posX: Double,
            attackState: AttackState? = null,
            health: Int = 100,
        ) = Player(
            id = id,
            name = "P$id",
            position = Position(posX, 0.0),
            nextMove = Movement(Direction(0.0, 0.0), Position(posX, 0.0), 0.0, 0.0),
            health = Health(health, 100),
            hurtBox = Rectangle(posX, 0.0, 50.0, 80.0),
            attackState = attackState,
        )

        fun world(vararg players: Player) =
            World(
                frameNumber = 0,
                players = players.associateBy { it.id },
                gameStatus = GameStatus.RUNNING,
            )

        "no hit when attack is in STARTUP phase" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = 0),
                ) // STARTUP
            val opponent = createPlayer(2, posX = 110.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[2]!!.health.current shouldBe 100
        }

        "no hit when attack is in RECOVERY phase" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = jab.startupFrames + jab.activeFrames),
                ) // RECOVERY
            val opponent = createPlayer(2, posX = 110.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[2]!!.health.current shouldBe 100
        }

        "hit registers when hitbox overlaps opponent hurtbox in ACTIVE phase" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = jab.startupFrames),
                ) // ACTIVE
            val opponent = createPlayer(2, posX = 110.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[2]!!.health.current shouldBe 90
        }

        "no hit when opponent is out of range" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = jab.startupFrames),
                ) // ACTIVE
            val opponent = createPlayer(2, posX = 500.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[2]!!.health.current shouldBe 100
        }

        "attack does not land twice on same activation" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = jab.startupFrames, hasLanded = true),
                )
            val opponent = createPlayer(2, posX = 110.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[2]!!.health.current shouldBe 100
        }

        "hasLanded is set to true after hit" {
            val attacker =
                createPlayer(
                    1,
                    posX = 100.0,
                    attackState = AttackState(jab, currentFrame = jab.startupFrames),
                )
            val opponent = createPlayer(2, posX = 110.0)
            val result = HitDetectionSystem.update(world(attacker, opponent))
            result.players[1]!!.attackState!!.hasLanded shouldBe true
        }
    })
