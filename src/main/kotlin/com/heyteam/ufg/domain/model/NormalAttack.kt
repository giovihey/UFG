package com.heyteam.ufg.domain.model

import java.awt.Button

data class NormalAttack(
    override val name: String,
    override val damage: Int,
    val buttonInput: Button,
): Technique
