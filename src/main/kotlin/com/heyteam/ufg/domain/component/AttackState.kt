package com.heyteam.ufg.domain.component

data class AttackState(
    val attack: Attack,
    val currentFrame: Int = 0,
    val hasLanded: Boolean = false,
) {
    val totalFrames = attack.startupFrames + attack.activeFrames + attack.recoveryFrames
    val phase: AttackPhase
        get() =
            when {
                currentFrame < attack.startupFrames -> AttackPhase.STARTUP
                currentFrame < attack.startupFrames + attack.activeFrames -> AttackPhase.ACTIVE
                else -> AttackPhase.RECOVERY
            }
    val isExpired: Boolean
        get() = currentFrame >= totalFrames
}

enum class AttackPhase { STARTUP, ACTIVE, RECOVERY }
