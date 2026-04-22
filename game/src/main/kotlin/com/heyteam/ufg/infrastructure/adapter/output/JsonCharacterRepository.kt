package com.heyteam.ufg.infrastructure.adapter.output

import com.heyteam.ufg.application.port.output.CharacterRepository
import com.heyteam.ufg.domain.component.Attack
import com.heyteam.ufg.domain.component.AttackCommand
import com.heyteam.ufg.domain.component.GameButton
import com.heyteam.ufg.domain.component.Health
import com.heyteam.ufg.domain.component.Rectangle
import com.heyteam.ufg.domain.entity.Character
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RectangleDto(
    val offsetX: Double,
    val offsetY: Double,
    val width: Double,
    val height: Double,
) {
    fun toDomain() = Rectangle(offsetX, offsetY, width, height)
}

@Serializable
private data class AttackDto(
    val name: String,
    val damage: Int,
    val buttonInput: GameButton,
    val hitBox: RectangleDto,
    val startupFrames: Int,
    val activeFrames: Int,
    val recoveryFrames: Int,
    val hitstunFrames: Int,
    val knockbackSpeed: Double,
) {
    fun toDomain() =
        Attack(
            name = name,
            damage = damage,
            buttonInput = buttonInput,
            hitBox = hitBox.toDomain(),
            startupFrames = startupFrames,
            activeFrames = activeFrames,
            recoveryFrames = recoveryFrames,
            hitstunFrames = hitstunFrames,
            knockbackSpeed = knockbackSpeed,
        )
}

@Serializable
private data class CharacterDto(
    val id: Int,
    val name: String,
    val maxHealth: Int,
    val walkSpeed: Double,
    val jumpSpeed: Double,
    val weight: Double,
    val defaultHurtbox: RectangleDto,
    val moveList: Map<AttackCommand, AttackDto>,
) {
    fun toDomain() =
        Character(
            id = id,
            name = name,
            maxHealth = Health(maxHealth, maxHealth),
            walkSpeed = walkSpeed,
            jumpSpeed = jumpSpeed,
            weight = weight,
            moveList = moveList.mapValues { it.value.toDomain() },
            defaultHurtbox = defaultHurtbox.toDomain(),
        )
}

class JsonCharacterRepository(
    resourcePaths: List<String> =
        listOf(
            "/characters/all_rounder.json",
            "/characters/rushdown.json",
            "/characters/heavy.json",
        ),
) : CharacterRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val characters: List<Character> =
        resourcePaths.map { path ->
            val text =
                requireNotNull(this::class.java.getResourceAsStream(path)) {
                    "Character resource not found: $path"
                }.bufferedReader().use { it.readText() }
            json.decodeFromString<CharacterDto>(text).toDomain()
        }

    override fun findById(id: Int): Character? = characters.firstOrNull { it.id == id }

    override fun findAll(): List<Character> = characters
}
