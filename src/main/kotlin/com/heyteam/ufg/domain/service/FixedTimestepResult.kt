package com.heyteam.ufg.domain.service

data class FixedTimestepResult(
    val deltaTime: Double,
    val steps: Int,
    val fixedDt: Double,
)
