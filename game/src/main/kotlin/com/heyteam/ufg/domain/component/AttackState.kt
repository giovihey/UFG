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

    fun activeHitBox(
        topLeft: Position,
        hurtboxWidth: Double,
        facing: Facing,
    ): Rectangle? {
        if (phase != AttackPhase.ACTIVE) return null
        val offsetX =
            if (facing == Facing.RIGHT) {
                attack.hitBox.x
            } else {
                hurtboxWidth - attack.hitBox.x - attack.hitBox.width
            }
        return attack.hitBox.copy(
            x = topLeft.x + offsetX,
            y = topLeft.y + attack.hitBox.y,
        )
    }
}

enum class AttackPhase { STARTUP, ACTIVE, RECOVERY }
