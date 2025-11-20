package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.Comparison
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface ComparisonRepository : JpaRepository<Comparison, Long> {

    /**
     * Find comparison by ID with all relationships eager-loaded
     * Used for detail views to prevent LazyInitializationException
     * Eagerly loads nested relationships: category and repository
     */
    @EntityGraph(attributePaths = [
        "user",
        "comparisonCategories",
        "comparisonCategories.category",
        "comparisonRepositories",
        "comparisonRepositories.repository"
    ])
    @Query("SELECT c FROM Comparison c WHERE c.id = :id")
    fun findByIdWithRelations(@Param("id") id: Long): Comparison?

    /**
     * Find comparison by session ID (for tracking async operations)
     * Eager loads user to prevent N+1 queries
     */
    @EntityGraph(attributePaths = ["user"])
    fun findBySessionId(sessionId: String): Comparison?

    /**
     * Find all comparisons with pagination and ordering
     * Eager loads user to prevent N+1 queries
     */
    @EntityGraph(attributePaths = ["user"])
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Comparison>

    /**
     * Find comparisons by user ID with pagination
     * User is already known, so no need to eager load
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Comparison>

    /**
     * Search comparisons by query text (fuzzy search)
     * Searches in: user_query, normalized_query, technologies, problem_domains
     * Eager loads user to prevent N+1 queries
     *
     * DEPRECATED: Use advancedSearch() for better performance with FULLTEXT index
     */
    @Query(
        """
        SELECT DISTINCT c FROM Comparison c
        LEFT JOIN FETCH c.user
        WHERE LOWER(c.userQuery) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.normalizedQuery) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.technologies) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.problemDomains) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY c.createdAt DESC
        """
    )
    fun searchByQuery(@Param("search") search: String, pageable: Pageable): Page<Comparison>

    /**
     * Advanced search using MySQL FULLTEXT index with n-gram parser + category search
     *
     * Searches in:
     * - FULLTEXT fields (n-gram indexed): user_query, normalized_query, technologies, problem_domains, architecture_patterns
     * - Category names (via JOIN to comparison_categories)
     *
     * Features:
     * - N-gram parser for short technical terms (api, db, job, orm, js, py, etc.)
     * - Synonym expansion handled by caller (SearchSynonymExpander)
     * - Relevance scoring via MATCH() AGAINST()
     * - Category search via LEFT JOIN
     * - Date range filtering (optional)
     * - Boolean mode wildcards supported (e.g., "+job* +queue*")
     *
     * @param searchTerms Space-separated search terms (already expanded with synonyms, formatted for BOOLEAN MODE)
     * @param startDate Optional start date filter (null = no filter)
     * @param endDate Optional end date filter (null = no filter)
     * @param pageable Pagination and sorting
     * @return Page of comparisons ordered by relevance
     */
    @Query(
        value = """
        SELECT DISTINCT c.*,
               MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
               AGAINST(:searchTerms IN BOOLEAN MODE) AS relevance
        FROM comparisons c
        LEFT JOIN users u ON c.user_id = u.id
        LEFT JOIN comparison_categories cc ON c.id = cc.comparison_id
        LEFT JOIN categories cat ON cc.category_id = cat.id
        WHERE (
            MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
            AGAINST(:searchTerms IN BOOLEAN MODE)
            OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :searchTerms, '%'))
        )
        AND (:startDate IS NULL OR c.created_at >= :startDate)
        AND (:endDate IS NULL OR c.created_at <= :endDate)
        ORDER BY relevance DESC, c.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM comparisons c
        LEFT JOIN comparison_categories cc ON c.id = cc.comparison_id
        LEFT JOIN categories cat ON cc.category_id = cat.id
        WHERE (
            MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
            AGAINST(:searchTerms IN BOOLEAN MODE)
            OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :searchTerms, '%'))
        )
        AND (:startDate IS NULL OR c.created_at >= :startDate)
        AND (:endDate IS NULL OR c.created_at <= :endDate)
        """,
        nativeQuery = true
    )
    fun advancedSearch(
        @Param("searchTerms") searchTerms: String,
        @Param("startDate") startDate: java.time.LocalDateTime?,
        @Param("endDate") endDate: java.time.LocalDateTime?,
        pageable: Pageable
    ): Page<Comparison>

    /**
     * Find most popular comparisons (by view count)
     * Eager loads user to prevent N+1 queries
     */
    @EntityGraph(attributePaths = ["user"])
    fun findAllByOrderByViewCountDesc(pageable: Pageable): Page<Comparison>

    /**
     * Find comparisons created within a date range
     * Eager loads user to prevent N+1 queries
     */
    @Query("""
        SELECT DISTINCT c FROM Comparison c
        LEFT JOIN FETCH c.user
        WHERE c.createdAt >= :startDate
        ORDER BY c.createdAt DESC
    """)
    fun findByCreatedAtAfter(@Param("startDate") startDate: java.time.LocalDateTime, pageable: Pageable): Page<Comparison>
}
