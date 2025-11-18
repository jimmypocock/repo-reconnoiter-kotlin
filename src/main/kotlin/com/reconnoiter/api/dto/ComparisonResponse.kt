package com.reconnoiter.api.dto

import com.reconnoiter.api.model.Comparison
import java.math.BigDecimal
import java.time.LocalDateTime

data class ComparisonResponse(
    val id: Long,
    val userQuery: String,
    val normalizedQuery: String?,
    val technologies: List<String>,
    val problemDomains: List<String>,
    val architecturePatterns: List<String>,
    val constraints: List<String>,
    val githubSearchQuery: String?,
    val reposComparedCount: Int?,
    val recommendedRepoFullName: String?,
    val recommendationReasoning: String?,
    val modelUsed: String?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val costUsd: BigDecimal?,
    val status: String?,
    val viewCount: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val userId: Long?
) {
    companion object {
        fun from(comparison: Comparison): ComparisonResponse {
            return ComparisonResponse(
                id = comparison.id!!,
                userQuery = comparison.userQuery,
                normalizedQuery = comparison.normalizedQuery,
                technologies = parseCommaSeparated(comparison.technologies),
                problemDomains = parseCommaSeparated(comparison.problemDomains),
                architecturePatterns = parseCommaSeparated(comparison.architecturePatterns),
                constraints = parseJsonArray(comparison.constraints),
                githubSearchQuery = comparison.githubSearchQuery,
                reposComparedCount = comparison.reposComparedCount,
                recommendedRepoFullName = comparison.recommendedRepoFullName,
                recommendationReasoning = comparison.recommendationReasoning,
                modelUsed = comparison.modelUsed,
                inputTokens = comparison.inputTokens,
                outputTokens = comparison.outputTokens,
                costUsd = comparison.costUsd,
                status = comparison.status,
                viewCount = comparison.viewCount,
                createdAt = comparison.createdAt,
                updatedAt = comparison.updatedAt,
                userId = comparison.user?.id
            )
        }

        private fun parseCommaSeparated(value: String?): List<String> {
            return value
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        }

        private fun parseJsonArray(jsonArray: String?): List<String> {
            return try {
                // Simple JSON array parsing - will improve with Jackson later
                jsonArray
                    ?.trim()
                    ?.removeSurrounding("[", "]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class ComparisonDetailResponse(
    val id: Long,
    val userQuery: String,
    val normalizedQuery: String?,
    val technologies: List<String>,
    val problemDomains: List<String>,
    val architecturePatterns: List<String>,
    val constraints: List<String>,
    val githubSearchQuery: String?,
    val reposComparedCount: Int?,
    val recommendedRepoFullName: String?,
    val recommendationReasoning: String?,
    val rankingResults: String?,
    val modelUsed: String?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val costUsd: BigDecimal?,
    val status: String?,
    val viewCount: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val userId: Long?,
    val categories: List<CategorySummary>,
    val repositories: List<RepositorySummary>
) {
    companion object {
        fun from(comparison: Comparison): ComparisonDetailResponse {
            return ComparisonDetailResponse(
                id = comparison.id!!,
                userQuery = comparison.userQuery,
                normalizedQuery = comparison.normalizedQuery,
                technologies = parseCommaSeparated(comparison.technologies),
                problemDomains = parseCommaSeparated(comparison.problemDomains),
                architecturePatterns = parseCommaSeparated(comparison.architecturePatterns),
                constraints = parseJsonArray(comparison.constraints),
                githubSearchQuery = comparison.githubSearchQuery,
                reposComparedCount = comparison.reposComparedCount,
                recommendedRepoFullName = comparison.recommendedRepoFullName,
                recommendationReasoning = comparison.recommendationReasoning,
                rankingResults = comparison.rankingResults,
                modelUsed = comparison.modelUsed,
                inputTokens = comparison.inputTokens,
                outputTokens = comparison.outputTokens,
                costUsd = comparison.costUsd,
                status = comparison.status,
                viewCount = comparison.viewCount,
                createdAt = comparison.createdAt,
                updatedAt = comparison.updatedAt,
                userId = comparison.user?.id,
                categories = comparison.comparisonCategories.map { CategorySummary.from(it) },
                repositories = comparison.comparisonRepositories
                    .sortedByDescending { it.ranking }
                    .map { RepositorySummary.from(it) }
            )
        }

        private fun parseCommaSeparated(value: String?): List<String> {
            return value
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        }

        private fun parseJsonArray(jsonArray: String?): List<String> {
            return try {
                jsonArray
                    ?.trim()
                    ?.removeSurrounding("[", "]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class CategorySummary(
    val id: Long,
    val name: String,
    val categoryType: String
) {
    companion object {
        fun from(comparisonCategory: com.reconnoiter.api.model.ComparisonCategory): CategorySummary {
            return CategorySummary(
                id = comparisonCategory.category.id!!,
                name = comparisonCategory.category.name,
                categoryType = comparisonCategory.category.categoryType
            )
        }
    }
}

data class RepositorySummary(
    val id: Long,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val stargazersCount: Int,
    val ranking: Int?,
    val score: Int?
) {
    companion object {
        fun from(comparisonRepository: com.reconnoiter.api.model.ComparisonRepository): RepositorySummary {
            return RepositorySummary(
                id = comparisonRepository.repository.id!!,
                fullName = comparisonRepository.repository.fullName,
                description = comparisonRepository.repository.description,
                htmlUrl = comparisonRepository.repository.htmlUrl,
                stargazersCount = comparisonRepository.repository.stargazersCount,
                ranking = comparisonRepository.ranking,
                score = comparisonRepository.score
            )
        }
    }
}
