package com.reconnoiter.api.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import java.time.LocalDateTime

import com.reconnoiter.api.repository.UserRepository

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * JWT Authentication Filter
 *
 * Validates JWT tokens from X-User-Token header for user-specific endpoints.
 * This filter runs AFTER ApiKeyAuthenticationFilter, so API key is already validated.
 *
 * Note: Authorization: Bearer header is used for API keys, not JWTs.
 * JWTs must be provided via X-User-Token header to avoid conflicts.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private const val USER_TOKEN_HEADER = "X-User-Token"
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null) {
                // Check if API key was validated (two-layer auth: API key + JWT)
                val apiKeyValidated = request.getAttribute("API_KEY_VALIDATED") as? Boolean ?: false

                if (!apiKeyValidated) {
                    sendErrorResponse(
                        response,
                        401,
                        "Missing or invalid API key - user endpoints require both API key and JWT",
                        "MISSING_API_KEY"
                    )
                    return
                }

                // validateToken() now throws exceptions instead of returning boolean
                val claims = jwtUtil.validateToken(token)
                val userId = claims.get("user_id", Integer::class.java).toLong()

                val user = userRepository.findById(userId).orElse(null)

                if (user != null) {
                    val authentication = UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        emptyList() // No roles/authorities for now
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    // User not found in database (JWT valid but user deleted)
                    sendErrorResponse(response, 401, "User not found", "USER_NOT_FOUND")
                    return
                }
            }
        } catch (e: ExpiredJwtException) {
            // Expected: Token expired - user needs to log in again
            sendErrorResponse(response, 401, "Token expired - please log in again", "TOKEN_EXPIRED")
            return
        } catch (e: SignatureException) {
            // Expected: Invalid signature - possible tampering
            sendErrorResponse(response, 401, "Invalid token signature", "INVALID_SIGNATURE")
            return
        } catch (e: MalformedJwtException) {
            // Expected: Malformed token - client bug or invalid format
            sendErrorResponse(response, 400, "Malformed token format", "MALFORMED_TOKEN")
            return
        } catch (e: IllegalArgumentException) {
            // Expected: Empty token or invalid claims
            sendErrorResponse(response, 400, "Invalid token", "INVALID_TOKEN")
            return
        }
        // Unexpected exceptions (config errors, etc.) bubble up to Sentry

        filterChain.doFilter(request, response)
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        // Only check X-User-Token header (Authorization: Bearer is used for API keys)
        val userToken = request.getHeader(USER_TOKEN_HEADER)
        if (userToken != null && userToken.isNotBlank()) {
            return userToken
        }

        return null
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        status: Int,
        message: String,
        errorCode: String
    ) {
        response.status = status
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """
            {
                "error": "${if (status == 401) "Unauthorized" else "Bad Request"}",
                "message": "$message",
                "errorCode": "$errorCode",
                "timestamp": "${LocalDateTime.now()}"
            }
            """.trimIndent()
        )
        response.writer.flush()
    }
}
