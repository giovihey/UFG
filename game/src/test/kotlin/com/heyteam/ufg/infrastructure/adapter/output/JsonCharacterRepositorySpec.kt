package com.heyteam.ufg.infrastructure.adapter.output

import com.heyteam.ufg.domain.component.AttackCommand
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class JsonCharacterRepositorySpec :
    StringSpec({

        val repo = JsonCharacterRepository()

        "loads all three bundled characters" {
            repo.findAll().map { it.name } shouldContainExactlyInAnyOrder listOf("Ryo", "Kaze", "Brom")
        }

        "each character has neutral and down variants of PUNCH and KICK" {
            repo.findAll().forEach { c ->
                c.moveList.keys shouldContain AttackCommand.NEUTRAL_PUNCH
                c.moveList.keys shouldContain AttackCommand.NEUTRAL_KICK
                c.moveList.keys shouldContain AttackCommand.DOWN_PUNCH
                c.moveList.keys shouldContain AttackCommand.DOWN_KICK
            }
        }

        "rushdown walks faster than heavy" {
            val rushdown = repo.findById(2).shouldNotBeNull()
            val heavy = repo.findById(3).shouldNotBeNull()
            heavy.walkSpeed shouldBeLessThan rushdown.walkSpeed
        }

        "heavy has more HP than rushdown" {
            val rushdown = repo.findById(2).shouldNotBeNull()
            val heavy = repo.findById(3).shouldNotBeNull()
            (heavy.maxHealth.max > rushdown.maxHealth.max) shouldBe true
        }

        "findById returns null for unknown id" {
            repo.findById(999) shouldBe null
        }
    })
