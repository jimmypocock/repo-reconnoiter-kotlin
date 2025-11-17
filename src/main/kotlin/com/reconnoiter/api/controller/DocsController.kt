package com.reconnoiter.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * API Documentation Controller
 * Serves OpenAPI specification in multiple formats
 * Documentation is public (no auth required)
 */
@RestController
@RequestMapping("")
class DocsController {

    private val yamlMapper = ObjectMapper(YAMLFactory())
    private val jsonMapper = ObjectMapper()

    /**
     * GET /openapi.json
     * Returns OpenAPI spec as JSON (for Swagger UI)
     */
    @GetMapping("/openapi.json", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun openapiJson(): ResponseEntity<Map<String, Any>> {
        return try {
            val resource = ClassPathResource("static/openapi.yml")
            val yamlContent = resource.inputStream.bufferedReader().use { it.readText() }

            // Parse YAML and convert to Map
            @Suppress("UNCHECKED_CAST")
            val openApiMap = yamlMapper.readValue(yamlContent, Map::class.java) as Map<String, Any>

            ResponseEntity.ok(openApiMap)
        } catch (e: Exception) {
            ResponseEntity.status(500).body(
                mapOf(
                    "error" to "Failed to load OpenAPI specification",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * GET /openapi.yml
     * Returns OpenAPI spec as YAML (for AI/programmatic access)
     */
    @GetMapping("/openapi.yml", produces = ["application/x-yaml"])
    fun openapiYaml(): ResponseEntity<String> {
        return try {
            val resource = ClassPathResource("static/openapi.yml")
            val yamlContent = resource.inputStream.bufferedReader().use { it.readText() }

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/x-yaml")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"openapi.yml\"")
                .body(yamlContent)
        } catch (e: Exception) {
            ResponseEntity.status(500).body("error: Failed to load OpenAPI specification")
        }
    }
}
