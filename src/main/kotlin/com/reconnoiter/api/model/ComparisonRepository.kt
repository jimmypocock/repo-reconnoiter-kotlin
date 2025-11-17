package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "comparison_repositories",
    indexes = [
        Index(name = "idx_comparison_repositories_comparison", columnList = "comparison_id"),
        Index(name = "idx_comparison_repositories_repository", columnList = "repository_id"),
        Index(name = "idx_comparison_repositories_rank", columnList = "comparison_id,rank")
    ]
)
data class ComparisonRepository(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_id", nullable = false)
    val comparison: Comparison,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @Column(name = "ranking")
    val ranking: Int? = null,

    @Column(name = "score")
    val score: Int? = null,

    @Column(name = "fit_reasoning", columnDefinition = "TEXT")
    val fitReasoning: String? = null,

    @Column(name = "pros", columnDefinition = "JSON")
    val pros: String? = "[]",

    @Column(name = "cons", columnDefinition = "JSON")
    val cons: String? = "[]",

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
