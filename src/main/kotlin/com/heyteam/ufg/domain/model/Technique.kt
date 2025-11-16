package com.heyteam.ufg.domain.model

sealed interface Technique {
    val name: String
    val damage: Int
}