package com.reconnoiter.api.controller

import com.reconnoiter.api.model.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * API v1 Comparisons Controller
 *
 * Endpoints:
 *   GET /comparisons - List comparisons with filtering, search, pagination
 *   POST /comparisons - Create comparison (async, requires user auth)
 *   GET /comparisons/:id - Show single comparison
 *   GET /comparisons/status/:session_id - Get creation status
 */
@RestController
@RequestMapping("/comparisons")
class ComparisonsController {

    /**
     * GET /comparisons
     * Returns paginated list of comparisons with optional filtering
     *
     * Query parameters:
     *   - search: Search term for fuzzy matching
     *   - date: Filter by date range (week, month)
     *   - sort: Sort order (recent, popular)
     *   - page: Page number (default: 1)
     *   - per_page: Items per page (default: 20, max: 100)
     */
    @GetMapping
    fun index(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "recent") sort: String,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "20", name = "per_page") perPage: Int
    ): ResponseEntity<*> {
        // TODO: Implement when Comparison model is ready
        val itemsPerPage = minOf(perPage, 100)

        return ResponseEntity.ok(
            mapOf(
                "data" to emptyList<Any>(),
                "meta" to mapOf(
                    "pagination" to mapOf(
                        "page" to page,
                        "per_page" to itemsPerPage,
                        "total_pages" to 0,
                        "total_count" to 0,
                        "next_page" to null,
                        "prev_page" to null
                    )
                ),
                "note" to "Comparison listing will be available once Comparison model is implemented"
            )
        )
    }

    /**
     * POST /comparisons
     * Creates a comparison asynchronously and returns session info for tracking
     *
     * Headers:
     *   X-User-Token: <JWT>
     *
     * Body:
     *   { "query": "Rails background job library" }
     *
     * Response (202 Accepted):
     *   {
     *     "session_id": "uuid",
     *     "status": "processing",
     *     "websocket_url": "ws://localhost:8080/cable",
     *     "status_url": "/api/v1/comparisons/status/uuid"
     *   }
     */
    @PostMapping
    fun create(
        @RequestBody request: CreateComparisonRequest,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<*> {
        // Check authentication
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf("error" to "Unauthorized - Please provide a valid X-User-Token header")
            )
        }

        // Validate query
        val query = request.query.trim()
        if (query.isEmpty() || query.length > 500) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                mapOf(
                    "error" to mapOf(
                        "message" to "Invalid query",
                        "details" to listOf("Query must be between 1 and 500 characters")
                    )
                )
            )
        }

        // Generate session ID for tracking
        val sessionId = UUID.randomUUID().toString()

        // TODO: Implement when ComparisonStatus model and background job infrastructure are ready

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "session_id" to sessionId,
                "status" to "processing",
                "websocket_url" to getWebsocketUrl(),
                "status_url" to "/api/v1/comparisons/status/$sessionId",
                "note" to "Comparison creation will be available once ComparisonStatus model and background job infrastructure are implemented"
            )
        )
    }

    /**
     * GET /comparisons/{id}
     * Returns a single comparison with full details
     *
     * Response (200 OK):
     *   {
     *     "data": {
     *       "id": 123,
     *       "user_query": "Rails background job library",
     *       "normalized_query": "rails background job",
     *       "technologies": ["Ruby", "Rails"],
     *       "problem_domains": ["Background Jobs"],
     *       "architecture_patterns": ["Queue-based"],
     *       "repos_compared_count": 5,
     *       "recommended_repo": "sidekiq/sidekiq",
     *       "view_count": 42,
     *       "created_at": "2025-11-12T00:00:00Z",
     *       "updated_at": "2025-11-12T00:00:00Z",
     *       "categories": [...],
     *       "repositories": [...]
     *     }
     *   }
     */
    @GetMapping("/{id}")
    fun show(@PathVariable id: Long): ResponseEntity<*> {
        // TODO: Implement when Comparison model is ready
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            mapOf(
                "error" to mapOf(
                    "message" to "Comparison not found",
                    "details" to listOf("Comparison with ID $id does not exist (Comparison model not yet implemented)")
                )
            )
        )
    }

    /**
     * GET /comparisons/status/{session_id}
     * Returns the current status of an async comparison creation
     *
     * Response (processing):
     *   { "status": "processing" }
     *
     * Response (completed):
     *   {
     *     "status": "completed",
     *     "comparison_id": 123,
     *     "comparison_url": "/comparisons/123"
     *   }
     *
     * Response (failed):
     *   {
     *     "status": "failed",
     *     "error_message": "No repositories found"
     *   }
     */
    @GetMapping("/status/{sessionId}")
    fun status(@PathVariable sessionId: String): ResponseEntity<*> {
        // TODO: Implement when ComparisonStatus model is ready
        return ResponseEntity.ok(
            mapOf(
                "status" to "processing",
                "note" to "Status tracking will be available once ComparisonStatus model is implemented"
            )
        )
    }

    private fun getWebsocketUrl(): String {
        // TODO: Read from environment or configuration
        return "ws://localhost:8080/cable"
    }
}

data class CreateComparisonRequest(
    val query: String
)
