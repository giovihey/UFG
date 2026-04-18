package com.heyteam.ufg.domain.component

enum class AttackCommand {
    NEUTRAL_PUNCH,
    DOWN_PUNCH,
    UP_PUNCH,
    LEFT_PUNCH,
    RIGHT_PUNCH,
    NEUTRAL_KICK,
    DOWN_KICK,
    UP_KICK,
    LEFT_KICK,
    RIGHT_KICK,
    ;

    companion object {
        fun of(
            direction: AttackDirection,
            button: AttackButton,
        ): AttackCommand = valueOf("${direction.name}_${button.name}")
    }
}

enum class AttackDirection { NEUTRAL, DOWN, UP, LEFT, RIGHT }

enum class AttackButton { PUNCH, KICK }
