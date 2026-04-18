package com.heyteam.ufg.infrastructure.adapter.network

import com.heyteam.ufg.application.port.output.AuthPort
import com.heyteam.ufg.domain.component.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * HttpAuthAdapter communicates with the Go auth service.
 *
 * Endpoints:
 *   POST /auth/login   → {username, password} → {token}
 *   POST /auth/register → {username, password} → {token}
 *
 * The token is a JWT. We decode its payload client-side to extract
 * userId and username, then store the full token in the Session.
 */
class HttpAuthAdapter(
    private val baseUrl: String,
) : AuthPort {
    // HttpClient is thread-safe and reusable. Create once, use forever.
    private val client = HttpClient.newHttpClient()

    /**
     * POST /auth/login
     * Sends credentials, receives JWT, extracts claims, returns Session.
     */
    override suspend fun login(
        username: String,
        password: String,
    ): Result<Session> =
        runCatching {
            withContext(Dispatchers.IO) {
                // ← moves blocking call off the main thread
                val response = executeAuthRequest("$baseUrl/auth/login", AuthRequest(username, password))
                val token = JSONObject(response).getString("token")
                val claims = decodeJwtPayload(token)
                Session(claims.userId, claims.username, token)
            }
        }

    /**
     * POST /auth/register
     * Same flow as login: send credentials, get token, extract claims, return Session.
     */
    override suspend fun register(
        username: String,
        password: String,
    ): Result<Session> =
        runCatching {
            val authRequest = AuthRequest(username, password)
            val response = executeAuthRequest("$baseUrl/auth/register", authRequest)

            val responseJson = JSONObject(response)
            val token = responseJson.getString("token")

            val claims = decodeJwtPayload(token)
            Session(claims.userId, claims.username, token)
        }

    /**
     * Helper: execute a POST request to the auth service.
     * Throws exception on non-2xx status code.
     *
     * @param url the full URL (e.g., "http://localhost:8080/auth/login")
     * @param authRequest the {username, password} to send
     * @return the response body as a string
     */
    private fun executeAuthRequest(
        url: String,
        authRequest: AuthRequest,
    ): String {
        // Serialize AuthRequest to JSON
        val requestBody =
            JSONObject()
                .apply {
                    put("username", authRequest.username)
                    put("password", authRequest.password)
                }.toString()

        // Build HTTP POST request
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

        // Send request, get response
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Check status code
        if (response.statusCode() !in 200..299) {
            // Try to parse error message from response body
            val errorBody =
                try {
                    JSONObject(response.body()).getString("error")
                } catch (e: Exception) {
                    response.body()
                }
            throw RuntimeException(
                "Auth service error (${response.statusCode()}): $errorBody",
            )
        }

        return response.body()
    }
}
