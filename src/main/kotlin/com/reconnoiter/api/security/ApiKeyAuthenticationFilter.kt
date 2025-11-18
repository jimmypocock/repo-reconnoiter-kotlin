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

@Component
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "

        // Public endpoints that don't require API key
        private val PUBLIC_PATHS = setOf(
            "/",
            "/actuator/health",
            "/actuator/info"
        )
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI.removePrefix(request.contextPath)

        // Skip API key validation for public endpoints
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"Unauthorized - Missing or invalid Authorization header","message":"Please provide a valid API key via 'Authorization: Bearer <API_KEY>' header"}""")
            return
        }

        val apiKey = authHeader.substring(BEARER_PREFIX.length).trim()

        // Validate API key
        val validatedKey = apiKeyService.validateApiKey(apiKey)

        if (validatedKey == null) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"Unauthorized - Invalid API key","message":"The provided API key is invalid or has been revoked"}""")
            return
        }

        // Set authentication in security context
        val authorities = listOf(SimpleGrantedAuthority("ROLE_API_USER"))
        val authentication = PreAuthenticatedAuthenticationToken(
            validatedKey,
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication

        // Store API key in request attribute for later use
        request.setAttribute("apiKey", validatedKey)

        filterChain.doFilter(request, response)
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

    private fun isPublicPath(path: String): Boolean {
        return PUBLIC_PATHS.any { publicPath -> path == publicPath || path.startsWith("$publicPath/") }
    }
}
