package com.heyteam.ufg.domain.component

data class Session(
    val userId: Int,
    val username: String,
    val accessToken: String,
)
