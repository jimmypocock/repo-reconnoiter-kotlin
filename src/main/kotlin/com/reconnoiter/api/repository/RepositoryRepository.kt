package com.reconnoiter.api.repository

import com.reconnoiter.api.model.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface RepositoryRepository : JpaRepository<Repository, Long> {
    fun findByGithubId(githubId: Long): Repository?
    fun findByFullName(fullName: String): Repository?
    fun findByLanguage(language: String, pageable: Pageable): Page<Repository>
    fun findByArchivedFalseAndDisabledFalse(pageable: Pageable): Page<Repository>
    fun findByStargazersCountGreaterThanEqual(minStars: Int, pageable: Pageable): Page<Repository>
}
