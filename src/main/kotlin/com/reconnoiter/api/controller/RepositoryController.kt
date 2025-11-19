package com.reconnoiter.api.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

import com.reconnoiter.api.dto.PagedResponse
import com.reconnoiter.api.dto.RepositoryResponse
import com.reconnoiter.api.repository.RepositoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ConditionalOnWebApplication
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
}

class RepositoryNotFoundException(message: String) : RuntimeException(message)
