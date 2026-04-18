package com.heyteam.ufg.infrastructure.adapter.network

import org.json.JSONObject
import java.util.Base64

/**
 * Decodes a JWT's payload without verifying the signature.
 * The JWT format is: header.payload.signature (each part is base64-encoded)
 *
 * This is safe because:
 * - We trust the server's signature (verified by HTTPS in production)
 * - We only read the payload; we don't trust it for security decisions
 * - On login, we verify the token with the server (no token = no session)
 *
 * @param token the JWT string from the server
 * @return JwtClaims if successful, throws exception on invalid format
 */
fun decodeJwtPayload(token: String): JwtClaims {
    val parts = token.split(".")
    if (parts.size != 3) {
        throw IllegalArgumentException("Invalid JWT format: expected 3 parts, got ${parts.size}")
    }

    val payloadBase64 = parts[1]
    // Add padding if needed (base64 decode requires length % 4 == 0)
    val paddedPayload =
        payloadBase64.padEnd(
            (payloadBase64.length + 3) / 4 * 4,
            '=',
        )

    val decodedBytes =
        try {
            Base64.getUrlDecoder().decode(paddedPayload)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Failed to decode JWT payload: ${e.message}")
        }

    val payloadJson = String(decodedBytes, Charsets.UTF_8)
    val obj = JSONObject(payloadJson)

    return JwtClaims(
        userId = obj.getInt("userId"),
        username = obj.getString("username"),
    )
}
