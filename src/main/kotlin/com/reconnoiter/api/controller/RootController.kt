package com.reconnoiter.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * API root endpoint for discoverability
 * Returns available endpoints and API information
 * Root endpoint is public (no auth required)
 */
@RestController
@RequestMapping("/")
class RootController {

    @GetMapping
    fun index(): ResponseEntity<RootResponse> {
        return ResponseEntity.ok(
            RootResponse(
                message = "Welcome to RepoReconnoiter API v1",
                version = "v1",
                note = "This endpoint is public and does not require authentication",
                endpoints = mapOf(
                    "comparisons" to EndpointInfo(
                        url = "/api/v1/comparisons",
                        methods = listOf("GET", "POST"),
                        description = "List and create repository comparisons",
                        authentication = "Required"
                    ),
                    "repositories" to EndpointInfo(
                        url = "/api/v1/repositories",
                        methods = listOf("GET", "POST"),
                        description = "List repositories and trigger deep analysis",
                        authentication = "Required"
                    ),
                    "profile" to EndpointInfo(
                        url = "/api/v1/profile",
                        methods = listOf("GET"),
                        description = "Get current user profile",
                        authentication = "Required (User Token)"
                    ),
                    "documentation" to DocumentationInfo(
                        openapiJson = "/api/v1/openapi.json",
                        openapiYaml = "/api/v1/openapi.yml",
                        description = "API documentation and OpenAPI specs",
                        authentication = "Not required"
                    )
                ),
                authentication = AuthenticationInfo(
                    note = "Most endpoints require authentication. This root endpoint does not.",
                    apiKey = AuthMethodInfo(
                        header = "Authorization",
                        format = "Bearer YOUR_API_KEY",
                        description = "Required for all endpoints except root and documentation"
                    ),
                    userToken = AuthMethodInfo(
                        header = "X-User-Token",
                        format = "YOUR_JWT_TOKEN",
                        description = "Required for user-specific endpoints (profile, creating comparisons/analyses)"
                    )
                )
            )
        )
    }
}

data class RootResponse(
    val message: String,
    val version: String,
    val note: String,
    val endpoints: Map<String, Any>,
    val authentication: AuthenticationInfo
)

data class EndpointInfo(
    val url: String,
    val methods: List<String>,
    val description: String,
    val authentication: String
)

data class DocumentationInfo(
    val openapiJson: String,
    val openapiYaml: String,
    val description: String,
    val authentication: String
)

data class AuthenticationInfo(
    val note: String,
    val apiKey: AuthMethodInfo,
    val userToken: AuthMethodInfo
)

data class AuthMethodInfo(
    val header: String,
    val format: String,
    val description: String
)
