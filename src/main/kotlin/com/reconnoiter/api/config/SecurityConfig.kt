package com.reconnoiter.api.config

import com.reconnoiter.api.security.ApiKeyAuthenticationFilter
import com.reconnoiter.api.security.JwtAuthenticationFilter
import com.reconnoiter.api.security.OAuth2LoginSuccessHandler
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints (no auth required at all)
                    .requestMatchers(
                        "/",
                        "/actuator/health",
                        "/actuator/info"
                    ).permitAll()
                    // All other endpoints require at least API key auth
                    // JWT filter will handle user-specific endpoints
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureUrl("/login?error=true")
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { _, response, _ ->
                        response.status = HttpServletResponse.SC_UNAUTHORIZED
                        response.contentType = "application/json"
                        response.writer.write("""{"error":"Unauthorized","message":"Please provide valid authentication headers"}""")
                    }
            }
            // Order matters: API Key filter first, then JWT filter
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(jwtAuthenticationFilter, ApiKeyAuthenticationFilter::class.java)

        return http.build()
    }
}
