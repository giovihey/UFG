package com.heyteam.ufg.domain.system

import com.heyteam.ufg.domain.component.Attack
import com.heyteam.ufg.domain.component.AttackPhase
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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class AttackSystemSpec :
    StringSpec({

        val jab =
            Attack(
                name = "Jab",
                damage = 5,
                buttonInput = GameButton.PUNCH,
                hitBox = Rectangle(0.0, 0.0, 40.0, 30.0),
                startupFrames = 4,
                activeFrames = 3,
                recoveryFrames = 8,
                hitstunFrames = 12,
                knockbackSpeed = 150.0,
            )

        fun createPlayer(attackState: AttackState? = null): Player =
            Player(
                id = 1,
                name = "P1",
                position = Position(100.0, 0.0),
                nextMove = Movement(Direction(0.0, 0.0), Position(100.0, 0.0), 0.0, 0.0),
                health = Health(100, 100),
                hurtBox = Rectangle(100.0, 0.0, 50.0, 80.0),
                attackState = attackState,
            )

        fun world(
            player: Player,
            status: GameStatus = GameStatus.RUNNING,
        ) = World(frameNumber = 0, players = mapOf(1 to player), gameStatus = status)

        "currentFrame advances each step" {
            val player = createPlayer(AttackState(attack = jab, currentFrame = 0))
            val result = AttackSystem.update(world(player))
            result.players[1]!!.attackState!!.currentFrame shouldBe 1
        }

        "attackState is cleared when expired" {
            val totalFrames = jab.startupFrames + jab.activeFrames + jab.recoveryFrames
            val player = createPlayer(AttackState(attack = jab, currentFrame = totalFrames))
            val result = AttackSystem.update(world(player))
            result.players[1]!!.attackState.shouldBeNull()
        }

        "phase is STARTUP on first frame" {
            val player = createPlayer(AttackState(attack = jab, currentFrame = 0))
            val result = AttackSystem.update(world(player))
            result.players[1]!!.attackState!!.phase shouldBe AttackPhase.STARTUP
        }

        "phase is ACTIVE when in active window" {
            // startupFrames = 4, so frame 4 is the first ACTIVE frame
            val player = createPlayer(AttackState(attack = jab, currentFrame = jab.startupFrames))
            val result = AttackSystem.update(world(player))
            result.players[1]!!.attackState!!.phase shouldBe AttackPhase.ACTIVE
        }

        "phase is RECOVERY after active window" {
            // startupFrames + activeFrames = 7, so frame 7 is first RECOVERY frame
            val player = createPlayer(AttackState(attack = jab, currentFrame = jab.startupFrames + jab.activeFrames))
            val result = AttackSystem.update(world(player))
            result.players[1]!!.attackState!!.phase shouldBe AttackPhase.RECOVERY
        }

        "does nothing when game is not RUNNING" {
            val player = createPlayer(AttackState(attack = jab, currentFrame = 0))
            val result = AttackSystem.update(world(player, GameStatus.ROUND_END))
            result.players[1]!!.attackState!!.currentFrame shouldBe 0
        }
    })
