package com.reconnoiter.api.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS Configuration
 *
 * Allows Next.js frontend (SSR) to make requests to the Kotlin API.
 * Configured via environment variable for production/development flexibility.
 *
 * Note: CORS is a browser-level protection. Server-to-server requests (curl, Postman)
 * are not affected. Real security comes from API key + JWT authentication.
 *
 * Only loaded in web mode (@ConditionalOnWebApplication).
 * Console mode (Gradle tasks, Spring Shell) skips this configuration.
 */
@Configuration
@ConditionalOnWebApplication
class CorsConfig(
    @Value("\${app.frontend.url}") private val frontendUrl: String
) : WebMvcConfigurer {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private val logger = LoggerFactory.getLogger(CorsConfig::class.java)
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    override fun addCorsMappings(registry: CorsRegistry) {
        logger.info("Configuring CORS for frontend URL: $frontendUrl")
        registry.addMapping("/api/**")
            .allowedOrigins(frontendUrl)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)

        // Also allow CORS for auth endpoints
        registry.addMapping("/auth/**")
            .allowedOrigins(frontendUrl)
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
