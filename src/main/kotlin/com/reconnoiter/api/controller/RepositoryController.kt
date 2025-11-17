package com.reconnoiter.api.controller

import com.reconnoiter.api.dto.PagedResponse
import com.reconnoiter.api.dto.RepositoryResponse
import com.reconnoiter.api.model.User
import com.reconnoiter.api.repository.RepositoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/repositories")
class RepositoryController(
    private val repositoryRepository: RepositoryRepository
) {

    @GetMapping
    fun index(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false, name = "min_stars") minStars: Int?,
        @RequestParam(required = false, defaultValue = "updated") sort: String,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "20", name = "per_page") perPage: Int
    ): ResponseEntity<PagedResponse<RepositoryResponse>> {
        // Cap per_page at 100
        val itemsPerPage = minOf(perPage, 100)

        // Spring Data uses 0-based page numbers
        val pageNumber = maxOf(0, page - 1)

        // Build sort criteria
        val sortCriteria = when (sort) {
            "stars" -> Sort.by(Sort.Direction.DESC, "stargazersCount")
            "created" -> Sort.by(Sort.Direction.DESC, "githubCreatedAt")
            else -> Sort.by(Sort.Direction.DESC, "githubUpdatedAt")
        }

        val pageable = PageRequest.of(pageNumber, itemsPerPage, sortCriteria)

        // For now, just get all active repositories
        // TODO: Add search and filtering logic
        val repositoriesPage = repositoryRepository.findByArchivedFalseAndDisabledFalse(pageable)

        val response = PagedResponse(
            data = repositoriesPage.content.map { RepositoryResponse.from(it) },
            page = page,
            perPage = itemsPerPage,
            totalPages = repositoriesPage.totalPages,
            totalCount = repositoriesPage.totalElements
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun show(@PathVariable id: Long): ResponseEntity<RepositoryResponse> {
        val repository = repositoryRepository.findById(id)
            .orElseThrow { RepositoryNotFoundException("Repository with ID $id not found") }

        return ResponseEntity.ok(RepositoryResponse.from(repository))
    }

    /**
     * POST /repositories/{id}/analyze
     * Triggers a deep analysis asynchronously and returns session info for tracking
     *
     * Requires JWT authentication
     *
     * Response (202 Accepted):
     *   {
     *     "session_id": "uuid",
     *     "status": "processing",
     *     "repository_id": 123,
     *     "websocket_url": "ws://localhost:8080/cable",
     *     "status_url": "/api/v1/repositories/status/uuid"
     *   }
     */
    @PostMapping("/{id}/analyze")
    fun analyze(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<*> {
        // Check authentication
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf("error" to "Unauthorized - Please provide a valid X-User-Token header")
            )
        }

        // Verify repository exists
        val repository = repositoryRepository.findById(id)
            .orElseThrow { RepositoryNotFoundException("Repository with ID $id not found") }

        // Generate session ID for tracking
        val sessionId = UUID.randomUUID().toString()

        // TODO: Implement when AnalysisStatus model and background job infrastructure are ready
        // For now, return placeholder response
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "session_id" to sessionId,
                "status" to "processing",
                "repository_id" to repository.id,
                "websocket_url" to getWebsocketUrl(),
                "status_url" to "/api/v1/repositories/status/$sessionId",
                "note" to "Deep analysis will be available once AnalysisStatus model and background job infrastructure are implemented"
            )
        )
    }

    /**
     * POST /repositories/analyze_by_url
     * Triggers a deep analysis for a GitHub URL (fetches repo if not in database)
     *
     * Body:
     *   {
     *     "url": "https://github.com/owner/repo"
     *   }
     *
     * Response (202 Accepted):
     *   {
     *     "session_id": "uuid",
     *     "status": "processing",
     *     "repository_id": 123,
     *     "websocket_url": "ws://localhost:8080/cable",
     *     "status_url": "/api/v1/repositories/status/uuid"
     *   }
     */
    @PostMapping("/analyze_by_url")
    fun analyzeByUrl(
        @RequestBody request: AnalyzeByUrlRequest,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<*> {
        // Check authentication
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf("error" to "Unauthorized - Please provide a valid X-User-Token header")
            )
        }

        // Validate URL parameter
        if (request.url.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "error" to mapOf(
                        "message" to "URL parameter is required",
                        "details" to listOf("Please provide a GitHub repository URL")
                    )
                )
            )
        }

        // TODO: Parse GitHub URL and fetch/create repository
        // TODO: Implement when GithubUrlParser and repository fetching are ready

        // Generate session ID for tracking
        val sessionId = UUID.randomUUID().toString()

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "session_id" to sessionId,
                "status" to "processing",
                "websocket_url" to getWebsocketUrl(),
                "status_url" to "/api/v1/repositories/status/$sessionId",
                "note" to "URL-based analysis will be available once GitHub URL parsing and repository fetching are implemented"
            )
        )
    }

    /**
     * GET /repositories/status/{session_id}
     * Returns the current status of an async deep analysis
     *
     * Response (processing):
     *   { "status": "processing" }
     *
     * Response (completed):
     *   {
     *     "status": "completed",
     *     "repository_id": 123,
     *     "repository_url": "/repositories/123"
     *   }
     *
     * Response (failed):
     *   {
     *     "status": "failed",
     *     "error_message": "Repository not found on GitHub"
     *   }
     */
    @GetMapping("/status/{sessionId}")
    fun status(@PathVariable sessionId: String): ResponseEntity<*> {
        // TODO: Implement when AnalysisStatus model is ready
        // For now, return placeholder response
        return ResponseEntity.ok(
            mapOf(
                "status" to "processing",
                "note" to "Status tracking will be available once AnalysisStatus model is implemented"
            )
        )
    }

    private fun getWebsocketUrl(): String {
        // TODO: Read from environment or configuration
        return "ws://localhost:8080/cable"
    }
}

data class AnalyzeByUrlRequest(
    val url: String
)

class RepositoryNotFoundException(message: String) : RuntimeException(message)
