package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "comparisons",
    indexes = [
        Index(name = "idx_comparisons_session", columnList = "session_id", unique = true),
        Index(name = "idx_comparisons_user", columnList = "user_id"),
        Index(name = "idx_comparisons_created_at", columnList = "created_at"),
        Index(name = "idx_comparisons_problem_domains", columnList = "problem_domains"),
        Index(name = "idx_comparisons_view_count", columnList = "view_count"),
        Index(name = "idx_comparisons_user_created", columnList = "user_id,created_at")
    ]
)
data class Comparison(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "session_id")
    val sessionId: String? = null,

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    val userQuery: String,

    @Column(name = "normalized_query")
    val normalizedQuery: String? = null,

    @Column(name = "technologies")
    val technologies: String? = null,

    @Column(name = "problem_domains")
    val problemDomains: String? = null,

    @Column(name = "architecture_patterns")
    val architecturePatterns: String? = null,

    @Column(name = "constraints", columnDefinition = "JSON")
    val constraints: String? = "[]",

    @Column(name = "github_search_query", columnDefinition = "TEXT")
    val githubSearchQuery: String? = null,

    @Column(name = "repos_compared_count")
    val reposComparedCount: Int? = null,

    @Column(name = "recommended_repo_full_name")
    val recommendedRepoFullName: String? = null,

    @Column(name = "recommendation_reasoning", columnDefinition = "TEXT")
    val recommendationReasoning: String? = null,

    @Column(name = "ranking_results", columnDefinition = "JSON")
    val rankingResults: String? = null,

    @Column(name = "model_used")
    val modelUsed: String? = null,

    @Column(name = "input_tokens")
    val inputTokens: Int? = null,

    @Column(name = "output_tokens")
    val outputTokens: Int? = null,

    @Column(name = "cost_usd", precision = 10, scale = 6)
    val costUsd: BigDecimal? = null,

    @Column(name = "status")
    val status: String? = null,

    @Column(name = "view_count")
    val viewCount: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "comparison", cascade = [CascadeType.ALL], orphanRemoval = true)
    val comparisonCategories: List<ComparisonCategory> = emptyList(),

    @OneToMany(mappedBy = "comparison", cascade = [CascadeType.ALL], orphanRemoval = true)
    val comparisonRepositories: List<ComparisonRepository> = emptyList()
)
