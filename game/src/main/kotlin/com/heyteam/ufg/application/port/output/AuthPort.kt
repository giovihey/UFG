package com.heyteam.ufg.application.port.output

import com.heyteam.ufg.domain.component.Session

interface AuthPort {
    suspend fun login(
        username: String,
        password: String,
    ): Result<Session>

    suspend fun register(
        username: String,
        password: String,
    ): Result<Session>
}
