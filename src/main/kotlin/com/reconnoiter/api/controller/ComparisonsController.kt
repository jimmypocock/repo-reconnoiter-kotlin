package com.reconnoiter.api.controller

import com.reconnoiter.api.dto.ComparisonDetailResponse
import com.reconnoiter.api.dto.ComparisonResponse
import com.reconnoiter.api.dto.PagedResponse
import com.reconnoiter.api.repository.ComparisonRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * API v1 Comparisons Controller
 *
 * Endpoints:
 *   GET /comparisons - List comparisons with filtering, search, pagination
 *   GET /comparisons/:id - Show single comparison
 */
@RestController
@RequestMapping("/comparisons")
class ComparisonsController(
    private val comparisonRepository: ComparisonRepository
) {

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
    ): ResponseEntity<PagedResponse<ComparisonResponse>> {
        // Cap per_page at 100
        val itemsPerPage = minOf(perPage, 100)

        // Spring Data uses 0-based page numbers
        val pageNumber = maxOf(0, page - 1)

        val pageable = PageRequest.of(pageNumber, itemsPerPage)

        // Fetch comparisons based on filters
        val comparisonsPage = when {
            // Search query provided
            !search.isNullOrBlank() -> {
                comparisonRepository.searchByQuery(search.trim(), pageable)
            }
            // Date filter provided
            date == "week" -> {
                val weekAgo = LocalDateTime.now().minusWeeks(1)
                comparisonRepository.findByCreatedAtAfter(weekAgo, pageable)
            }
            date == "month" -> {
                val monthAgo = LocalDateTime.now().minusMonths(1)
                comparisonRepository.findByCreatedAtAfter(monthAgo, pageable)
            }
            // Sort by popularity
            sort == "popular" -> {
                comparisonRepository.findAllByOrderByViewCountDesc(pageable)
            }
            // Default: sort by recent
            else -> {
                comparisonRepository.findAllByOrderByCreatedAtDesc(pageable)
            }
        }

        val response = PagedResponse(
            data = comparisonsPage.content.map { ComparisonResponse.from(it) },
            page = page,
            perPage = itemsPerPage,
            totalPages = comparisonsPage.totalPages,
            totalCount = comparisonsPage.totalElements
        )

        return ResponseEntity.ok(response)
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
    fun show(@PathVariable id: Long): ResponseEntity<ComparisonDetailResponse> {
        val comparison = comparisonRepository.findById(id)
            .orElseThrow { ComparisonNotFoundException("Comparison with ID $id not found") }

        return ResponseEntity.ok(ComparisonDetailResponse.from(comparison))
    }
}

class ComparisonNotFoundException(message: String) : RuntimeException(message)
