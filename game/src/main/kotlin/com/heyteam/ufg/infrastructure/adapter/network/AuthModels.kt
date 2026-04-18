package com.heyteam.ufg.infrastructure.adapter.network

/**
 * Request body for POST /auth/login and POST /auth/register
 * Matches the authRequest struct in the Go service.
 */
data class AuthRequest(
    val username: String,
    val password: String,
)

/**
 * Response body from POST /auth/login and POST /auth/register
 * Matches the authResponse struct in the Go service.
 */
data class AuthResponse(
    val token: String,
)

/**
 * Claims extracted from the JWT payload (via decoding, not verification).
 * The JWT is: header.payload.signature
 * The payload is base64-encoded JSON with userId and username.
 * We decode it client-side to extract these fields.
 *
 * Matches the Claims struct in the Go token.go
 */
data class JwtClaims(
    val userId: Int,
    val username: String,
)
