package com.reconnoiter.api.config

import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Security Headers Configuration
 *
 * Implements OWASP-recommended HTTP security headers for API responses.
 * Matches Rails app security configuration for consistency across services.
 *
 * Target: A+ rating on securityheaders.com
 *
 * Headers configured:
 * - Content-Security-Policy (CSP) - Prevents XSS, injection attacks
 * - X-Frame-Options - Prevents clickjacking
 * - X-Content-Type-Options - Prevents MIME sniffing
 * - X-XSS-Protection - Legacy XSS filter (defense in depth)
 * - Referrer-Policy - Controls referrer information leakage
 * - Permissions-Policy - Disables unnecessary browser features
 * - Strict-Transport-Security (HSTS) - Forces HTTPS (production only)
 *
 * Only loaded in web mode (@ConditionalOnWebApplication).
 * Console mode (Gradle tasks, Spring Shell) skips this configuration.
 */
@Configuration
@ConditionalOnWebApplication
class SecurityHeadersConfig {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        // Content Security Policy for API-only backend
        // Stricter than Rails (no scripts/styles needed for JSON API)
        private const val CSP_POLICY = "default-src 'none'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'"

        // Permissions Policy - disable all browser features for API
        private const val PERMISSIONS_POLICY = "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "gyroscope=(), " +
            "accelerometer=(), " +
            "ambient-light-sensor=(), " +
            "autoplay=(), " +
            "encrypted-media=(), " +
            "fullscreen=(), " +
            "picture-in-picture=(), " +
            "screen-wake-lock=(), " +
            "web-share=()"

        // HSTS - 1 year, include subdomains (production only)
        private const val HSTS_HEADER = "max-age=31536000; includeSubDomains"
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    @Bean
    fun securityHeadersFilter(): Filter {
        return Filter { request, response, chain ->
            val httpRequest = request as HttpServletRequest
            val httpResponse = response as HttpServletResponse

            // Content Security Policy - Prevents XSS and injection attacks
            httpResponse.setHeader("Content-Security-Policy", CSP_POLICY)

            // X-Frame-Options - Prevents clickjacking (deny all framing)
            httpResponse.setHeader("X-Frame-Options", "DENY")

            // X-Content-Type-Options - Prevents MIME sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff")

            // X-XSS-Protection - Legacy XSS filter (defense in depth)
            // Note: Modern browsers rely on CSP, but this provides backward compatibility
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block")

            // Referrer-Policy - Control referrer information sent to external sites
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")

            // Permissions-Policy - Disable unnecessary browser features
            httpResponse.setHeader("Permissions-Policy", PERMISSIONS_POLICY)

            chain.doFilter(request, response)
        }
    }

    /**
     * HSTS (HTTP Strict Transport Security)
     *
     * Forces browsers to only use HTTPS for all future requests.
     * Only enabled in production (requires valid SSL certificate).
     *
     * WARNING: Do not enable in development - will break localhost HTTP access!
     */
    @Bean
    @Profile("prod")
    fun hstsFilter(): Filter {
        return Filter { request, response, chain ->
            val httpResponse = response as HttpServletResponse
            httpResponse.setHeader("Strict-Transport-Security", HSTS_HEADER)
            chain.doFilter(request, response)
        }
    }
}
