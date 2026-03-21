package com.heyteam.ufg.domain.component

data class PlayerPhysicsState(
    val state: PlayerState = PlayerState.IDLE,
    //TODO() add the knockback, hitstun, blockstun
)