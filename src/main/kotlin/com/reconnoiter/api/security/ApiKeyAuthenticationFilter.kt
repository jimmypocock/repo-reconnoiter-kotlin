package com.reconnoiter.api.security

import com.reconnoiter.api.service.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.LocalDateTime

/**
 * API Key Authentication Filter
 *
 * Validates API keys from Authorization: Bearer header for API access control.
 * This filter runs BEFORE JwtAuthenticationFilter.
 *
 * API key validation provides specific error messages:
 * - Missing header: Let Spring Security handle (permitAll vs authenticated)
 * - Malformed header: 400 Bad Request
 * - Invalid/revoked key: 401 Unauthorized with specific reason
 *
 * Valid API keys set authentication in SecurityContext.
 */
@Component
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        // No Authorization header → continue (let SecurityConfig decide if auth required)
        if (authHeader == null) {
            filterChain.doFilter(request, response)
            return
        }

        // Malformed Authorization header (not "Bearer ...")
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            sendErrorResponse(
                response,
                400,
                "Malformed Authorization header - expected format: 'Bearer <api_key>'",
                "MALFORMED_HEADER"
            )
            return
        }

        val apiKey = authHeader.substring(BEARER_PREFIX.length).trim()

        // Empty API key after "Bearer "
        if (apiKey.isEmpty()) {
            sendErrorResponse(
                response,
                400,
                "Empty API key - provide a valid key after 'Bearer '",
                "EMPTY_API_KEY"
            )
            return
        }

        // Validate API key
        val validatedKey = apiKeyService.validateApiKey(apiKey)

        if (validatedKey == null) {
            // Invalid or revoked API key
            sendErrorResponse(
                response,
                401,
                "Invalid or revoked API key - please check your credentials",
                "INVALID_API_KEY"
            )
            return
        }

        // Valid API key → set authentication
        val authorities = listOf(SimpleGrantedAuthority("ROLE_API_USER"))
        val authentication = PreAuthenticatedAuthenticationToken(
            validatedKey,
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication

        // Store API key in request attribute for later use (rate limiting, logging, etc.)
        request.setAttribute("apiKey", validatedKey)

        // Set flag for JWT filter to verify (ensures both API key AND JWT for user endpoints)
        request.setAttribute("API_KEY_VALIDATED", true)

        filterChain.doFilter(request, response)
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

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
