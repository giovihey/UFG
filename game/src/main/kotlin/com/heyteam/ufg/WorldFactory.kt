package com.heyteam.ufg

import com.heyteam.ufg.application.port.output.CharacterRepository
import com.heyteam.ufg.domain.component.Direction
import com.heyteam.ufg.domain.component.Facing
import com.heyteam.ufg.domain.component.Movement
import com.heyteam.ufg.domain.component.PlayerPhysicsState
import com.heyteam.ufg.domain.component.Position
import com.heyteam.ufg.domain.entity.Character
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

fun createWorld(characters: CharacterRepository): World {
    val p1Character = requireNotNull(characters.findById(P1_CHARACTER_ID))
    val p2Character = requireNotNull(characters.findById(P2_CHARACTER_ID))
    val p1 = spawnPlayer(id = 1, name = "P1", startX = P1_START_X, character = p1Character, facing = Facing.RIGHT)
    val p2 = spawnPlayer(id = 2, name = "P2", startX = P2_START_X, character = p2Character, facing = Facing.LEFT)
    return World(frameNumber = 0L, players = mapOf(1 to p1, 2 to p2))
}

/**
 * Practice-mode world: [roundTimer] is [Int.MAX_VALUE] so [GameLogic.applyGameRules]
 * never triggers [GameStatus.ROUND_END] — the practice loop runs until the player
 * cancels or an opponent is found, whichever comes first.
 *
 * The timer displayed in [practiceScreen] is a Compose count-up, independent of this
 * value — it shows how long the player has been searching, not a game countdown.
 */
fun createPracticeWorld(characters: CharacterRepository): World =
    createWorld(characters)
        .copy(roundTimer = Int.MAX_VALUE)

private fun spawnPlayer(
    id: Int,
    name: String,
    startX: Double,
    character: Character,
    facing: Facing,
): Player =
    Player(
        id = id,
        name = name,
        position = Position(startX, 0.0),
        nextMove =
            Movement(
                direction = Direction(0.0, 0.0),
                position = Position(startX, 0.0),
                speedX = 0.0,
                speedY = 0.0,
            ),
        health = character.maxHealth,
        hurtBox = character.defaultHurtbox,
        character = character,
        physicsState = PlayerPhysicsState(facing = facing),
    )
