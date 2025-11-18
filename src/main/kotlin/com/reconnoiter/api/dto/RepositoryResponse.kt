package com.reconnoiter.api.dto

import com.reconnoiter.api.entity.Repository
import java.time.LocalDateTime

data class RepositoryResponse(
    val id: Long,
    val githubId: Long,
    val fullName: String,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val language: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val watchersCount: Int,
    val openIssuesCount: Int,
    val topics: List<String>,
    val license: String?,
    val ownerLogin: String?,
    val ownerAvatarUrl: String?,
    val githubCreatedAt: LocalDateTime?,
    val githubUpdatedAt: LocalDateTime?,
    val githubPushedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(repository: Repository): RepositoryResponse {
            return RepositoryResponse(
                id = repository.id!!,
                githubId = repository.githubId,
                fullName = repository.fullName,
                name = repository.name,
                description = repository.description,
                htmlUrl = repository.htmlUrl,
                language = repository.language,
                stargazersCount = repository.stargazersCount,
                forksCount = repository.forksCount,
                watchersCount = repository.watchersCount,
                openIssuesCount = repository.openIssuesCount,
                topics = parseTopics(repository.topics),
                license = repository.license,
                ownerLogin = repository.ownerLogin,
                ownerAvatarUrl = repository.ownerAvatarUrl,
                githubCreatedAt = repository.githubCreatedAt,
                githubUpdatedAt = repository.githubUpdatedAt,
                githubPushedAt = repository.githubPushedAt,
                createdAt = repository.createdAt,
                updatedAt = repository.updatedAt
            )
        }

        private fun parseTopics(topicsJson: String): List<String> {
            return try {
                // Simple JSON array parsing - will improve with Jackson later
                topicsJson
                    .trim()
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class PagedResponse<T>(
    val data: List<T>,
    val page: Int,
    val perPage: Int,
    val totalPages: Int,
    val totalCount: Long
)
