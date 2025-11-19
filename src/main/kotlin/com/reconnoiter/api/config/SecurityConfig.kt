package com.reconnoiter.api.config

import jakarta.servlet.http.HttpServletResponse

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

import com.reconnoiter.api.security.ApiKeyAuthenticationFilter
import com.reconnoiter.api.security.JwtAuthenticationFilter
import com.reconnoiter.api.security.OAuth2LoginSuccessHandler

/**
 * Security Configuration
 *
 * Implements two-layer authentication:
 * 1. API Key (Layer 1): Validates all requests come from trusted Next.js app
 * 2. JWT (Layer 2): Identifies users for user-specific endpoints
 *
 * Public endpoints (no auth): health, actuator, OpenAPI docs
 * API key only: GET repositories, comparisons (read-only data)
 * API key + JWT: POST comparisons (user-specific actions)
 *
 * Note: Only loaded in web mode (@ConditionalOnWebApplication)
 * Console mode (Gradle tasks, Spring Shell) skips this configuration
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
class SecurityConfig(
    private val apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { } // Enable CORS (uses CorsConfig)
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints (no auth required at all)
                    .requestMatchers(
                        "/",
                        "/actuator/health",
                        "/actuator/info",
                        "/openapi.json",
                        "/openapi.yml",
                        "/test/**"
                    ).permitAll()
                    // All other endpoints require at least API key auth
                    // JWT filter will handle user-specific endpoints
                    .anyRequest().authenticated()
            }
            // OAuth2 login flow (alternative auth method for development/testing)
            // Primary flow is: Next.js handles OAuth → POST /auth/exchange → JWT
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureUrl("/login?error=true")
            }
            .exceptionHandling { exceptions ->
                exceptions
                    // 401 Unauthorized - missing/invalid credentials
                    .authenticationEntryPoint { _, response, _ ->
                        response.status = HttpServletResponse.SC_UNAUTHORIZED
                        response.contentType = "application/json"
                        response.writer.write("""{"error":"Unauthorized","message":"Please provide valid authentication headers"}""")
                    }
                    // 403 Forbidden - authenticated but insufficient permissions
                    .accessDeniedHandler { _, response, _ ->
                        response.status = HttpServletResponse.SC_FORBIDDEN
                        response.contentType = "application/json"
                        response.writer.write("""{"error":"Forbidden","message":"You do not have permission to access this resource"}""")
                    }
            }
            // Order matters: API Key filter first, then JWT filter
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(jwtAuthenticationFilter, ApiKeyAuthenticationFilter::class.java)

        return http.build()
    }
}
