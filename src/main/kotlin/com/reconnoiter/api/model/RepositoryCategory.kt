package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "repository_categories",
    indexes = [
        Index(name = "idx_repo_categories_unique", columnList = "repository_id,category_id", unique = true),
        Index(name = "idx_repo_categories_repository", columnList = "repository_id"),
        Index(name = "idx_repo_categories_category", columnList = "category_id"),
        Index(name = "idx_repo_categories_confidence", columnList = "confidence_score")
    ]
)
data class RepositoryCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @Column(name = "confidence_score")
    val confidenceScore: Float? = null,

    @Column(name = "assigned_by")
    val assignedBy: String = "ai", // ai, manual, github_topics

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
