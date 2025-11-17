package com.reconnoiter.api.controller

import com.reconnoiter.api.model.User
import com.reconnoiter.api.repository.RepositoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * API v1 Admin Controller
 * Requires user JWT authentication + admin role
 *
 * Endpoints:
 *   GET /admin/stats - Get platform-wide statistics
 */
@RestController
@RequestMapping("/admin")
class AdminController(
    private val repositoryRepository: RepositoryRepository
) {

    /**
     * GET /admin/stats
     * Returns platform-wide statistics and AI spending information
     *
     * Headers:
     *   X-User-Token: <JWT>
     *
     * Response (200 OK):
     *   {
     *     "data": {
     *       "ai_spending": { ... },
     *       "budget": { ... },
     *       "platform": { ... },
     *       "spend_by_model": [...],
     *       "spend_by_user": [...]
     *     }
     *   }
     */
    @GetMapping("/stats")
    fun stats(@AuthenticationPrincipal user: User?): ResponseEntity<*> {
        // Check authentication
        if (user == null) {
            return ResponseEntity.status(401).body(
                mapOf(
                    "error" to "Unauthorized",
                    "message" to "Please provide a valid X-User-Token header"
                )
            )
        }

        // Check admin role
        if (!user.admin) {
            return ResponseEntity.status(403).body(
                mapOf(
                    "error" to "Forbidden",
                    "message" to "Admin access required - You must be an admin to access this endpoint"
                )
            )
        }

        // Gather statistics
        val repositoriesCount = repositoryRepository.count()

        // Return statistics
        // TODO: Implement full statistics when AiCost and Comparison models are available
        return ResponseEntity.ok(
            mapOf(
                "data" to mapOf(
                    "ai_spending" to mapOf(
                        "today" to 0.00,
                        "this_week" to 0.00,
                        "this_month" to 0.00,
                        "total" to 0.00,
                        "projected_month" to 0.00
                    ),
                    "budget" to mapOf(
                        "monthly_limit" to 10.0,
                        "remaining" to 10.0,
                        "percentage_used" to 0.0,
                        "status" to "healthy"
                    ),
                    "platform" to mapOf(
                        "comparisons_count" to 0, // TODO: Add when Comparison model exists
                        "repositories_count" to repositoriesCount,
                        "total_views" to 0 // TODO: Implement view tracking
                    ),
                    "spend_by_model" to emptyList<Map<String, Any>>(), // TODO: Add when AiCost model exists
                    "spend_by_user" to emptyList<Map<String, Any>>() // TODO: Add when AiCost model exists
                ),
                "note" to "Full statistics will be available once AiCost and Comparison models are implemented"
            )
        )
    }
}
