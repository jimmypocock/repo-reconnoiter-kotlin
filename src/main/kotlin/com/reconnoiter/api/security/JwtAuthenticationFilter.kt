package com.reconnoiter.api.security

import com.reconnoiter.api.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null && jwtUtil.validateToken(token)) {
                val userId = jwtUtil.getUserIdFromToken(token)

                if (userId != null) {
                    val user = userRepository.findById(userId).orElse(null)

                    if (user != null) {
                        val authentication = UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            emptyList() // No roles/authorities for now
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        // Check X-User-Token header (Rails API convention)
        val userToken = request.getHeader("X-User-Token")
        if (userToken != null && userToken.isNotBlank()) {
            return userToken
        }

        // Fallback to Authorization: Bearer header (standard)
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        return null
    }
}
