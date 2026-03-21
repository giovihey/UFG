package com.heyteam.ufg.application.service

data class FixedTimestepResult(
    val deltaTime: Double,
    val steps: Int,
    val fixedDt: Double,
)
