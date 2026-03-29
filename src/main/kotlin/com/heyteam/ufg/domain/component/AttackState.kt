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

    fun activeHitBox(position: Position): Rectangle? {
        if (phase != AttackPhase.ACTIVE) return null
        return attack.hitBox.copy(
            x = position.x + attack.hitBox.x,
            y = position.y + attack.hitBox.y,
        )
    }
}

enum class AttackPhase { STARTUP, ACTIVE, RECOVERY }
