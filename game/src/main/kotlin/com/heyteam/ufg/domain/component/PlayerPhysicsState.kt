package com.heyteam.ufg.domain.component

data class PlayerPhysicsState(
    val state: PlayerState = PlayerState.IDLE,
    val hitstunFramesRemaining: Int = 0,
    val isCrouching: Boolean = false,
    val facing: Facing = Facing.RIGHT,
)
