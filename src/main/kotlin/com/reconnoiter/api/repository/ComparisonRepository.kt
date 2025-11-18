package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.Comparison
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface ComparisonRepository : JpaRepository<Comparison, Long> {

    /**
     * Find comparison by session ID (for tracking async operations)
     */
    fun findBySessionId(sessionId: String): Comparison?

    /**
     * Find all comparisons with pagination and ordering
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Comparison>

    /**
     * Find comparisons by user ID with pagination
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Comparison>

    /**
     * Search comparisons by query text (fuzzy search)
     * Searches in: user_query, normalized_query, technologies, problem_domains
     */
    @Query(
        """
        SELECT c FROM Comparison c
        WHERE LOWER(c.userQuery) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.normalizedQuery) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.technologies) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.problemDomains) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY c.createdAt DESC
        """
    )
    fun searchByQuery(@Param("search") search: String, pageable: Pageable): Page<Comparison>

    /**
     * Find most popular comparisons (by view count)
     */
    fun findAllByOrderByViewCountDesc(pageable: Pageable): Page<Comparison>

    /**
     * Find comparisons created within a date range
     */
    @Query("SELECT c FROM Comparison c WHERE c.createdAt >= :startDate ORDER BY c.createdAt DESC")
    fun findByCreatedAtAfter(@Param("startDate") startDate: java.time.LocalDateTime, pageable: Pageable): Page<Comparison>
}
