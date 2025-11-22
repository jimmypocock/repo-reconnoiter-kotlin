package com.reconnoiter.api.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

import com.reconnoiter.api.dto.ComparisonDetailResponse
import com.reconnoiter.api.dto.ComparisonResponse
import com.reconnoiter.api.dto.PagedResponse
import com.reconnoiter.api.repository.ComparisonRepository
import com.reconnoiter.api.service.SearchSynonymExpander
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
@ConditionalOnWebApplication
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
     *   - search: Search term with synonym expansion and FULLTEXT matching
     *   - start_date: Filter by start date (ISO 8601 format)
     *   - end_date: Filter by end date (ISO 8601 format)
     *   - date: Filter by date range shorthand (week, month) - deprecated, use start_date/end_date
     *   - sort: Sort order (recent, popular)
     *   - page: Page number (default: 1)
     *   - per_page: Items per page (default: 20, max: 100)
     */
    @GetMapping
    fun index(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, name = "start_date") startDate: String?,
        @RequestParam(required = false, name = "end_date") endDate: String?,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "recent") sort: String,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "20", name = "per_page") perPage: Int
    ): ResponseEntity<PagedResponse<ComparisonResponse>> {
        // Validate search query length (max 255 characters - standard HTML input limit)
        if (!search.isNullOrBlank() && search.length > 255) {
            throw InvalidSearchQueryException("Search query too long (max 255 characters)")
        }

        // Cap per_page at 100
        val itemsPerPage = minOf(perPage, 100)

        // Spring Data uses 0-based page numbers
        val pageNumber = maxOf(0, page - 1)

        val pageable = PageRequest.of(pageNumber, itemsPerPage)

        // Parse date range if provided
        val parsedStartDate = when {
            !startDate.isNullOrBlank() -> LocalDateTime.parse(startDate)
            date == "week" -> LocalDateTime.now().minusWeeks(1)
            date == "month" -> LocalDateTime.now().minusMonths(1)
            else -> null
        }

        val parsedEndDate = if (!endDate.isNullOrBlank()) {
            LocalDateTime.parse(endDate)
        } else {
            null
        }

        // Fetch comparisons based on filters
        val comparisonsPage = when {
            // Search query provided - use advanced search with synonym expansion
            !search.isNullOrBlank() -> {
                // Expand search terms with synonyms
                val expandedTerms = SearchSynonymExpander.expandQuery(search.trim())

                // Format for MySQL BOOLEAN MODE with OR logic: "term1* term2* term3*"
                // Note: No '+' prefix because synonyms are alternatives (OR), not requirements (AND)
                val booleanQuery = expandedTerms.joinToString(" ") { "$it*" }

                comparisonRepository.advancedSearch(
                    searchTerms = booleanQuery,
                    startDate = parsedStartDate,
                    endDate = parsedEndDate,
                    pageable = pageable
                )
            }
            // Date filter without search
            parsedStartDate != null -> {
                comparisonRepository.findByCreatedAtAfter(parsedStartDate, pageable)
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
        val comparison = comparisonRepository.findByIdWithRelations(id)
            ?: throw ComparisonNotFoundException("Comparison with ID $id not found")

        return ResponseEntity.ok(ComparisonDetailResponse.from(comparison))
    }
}

class ComparisonNotFoundException(message: String) : RuntimeException(message)

class InvalidSearchQueryException(message: String) : RuntimeException(message)
