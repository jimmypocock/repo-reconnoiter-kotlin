package com.reconnoiter.api.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

import com.fasterxml.jackson.annotation.JsonProperty
import com.reconnoiter.api.service.AuthExchangeResult
import com.reconnoiter.api.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * API v1 Authentication Controller
 * Handles GitHub OAuth token exchange for JWT tokens
 *
 * Endpoints:
 *   POST /auth/token - Exchange GitHub OAuth token for JWT
 */
@ConditionalOnWebApplication
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * POST /auth/token
     * Exchange GitHub OAuth token for JWT
     *
     * Request body:
     *   { "github_token": "gho_..." }
     *
     * Response:
     *   {
     *     "jwt": "eyJ...",
     *     "user": {
     *       "id": 1,
     *       "github_id": 12345,
     *       "github_username": "username",
     *       "email": "user@example.com",
     *       "avatar_url": "https://...",
     *       "name": "User Name",
     *       "admin": false
     *     }
     *   }
     */
    @PostMapping("/token")
    fun exchange(@RequestBody request: ExchangeRequest): ResponseEntity<*> {
        if (request.githubToken.isBlank()) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(
                    message = "GitHub token required",
                    errors = listOf("Missing github_token in request body")
                )
            )
        }

        return when (val result = authService.exchangeGitHubToken(request.githubToken)) {
            is AuthExchangeResult.Success -> ResponseEntity.ok(
                ExchangeResponse(
                    jwt = result.jwt,
                    user = UserResponse(
                        id = result.user.id,
                        githubId = result.user.githubId,
                        githubUsername = result.user.githubUsername,
                        email = result.user.email,
                        avatarUrl = result.user.avatarUrl,
                        name = result.user.name,
                        admin = result.user.admin
                    )
                )
            )
            is AuthExchangeResult.Error -> {
                val status = when (result.message) {
                    "Invalid GitHub token" -> 401
                    "Access denied" -> 403
                    else -> 400
                }
                ResponseEntity.status(status).body(
                    ErrorResponse(
                        message = result.message,
                        errors = listOf(result.details)
                    )
                )
            }
        }
    }
}

data class ExchangeRequest(
    @JsonProperty("github_token")
    val githubToken: String
)

data class ExchangeResponse(
    val jwt: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val githubId: Long?,
    val githubUsername: String?,
    val email: String,
    val avatarUrl: String?,
    val name: String?,
    val admin: Boolean
)

data class ErrorResponse(
    val message: String,
    val errors: List<String>
)
