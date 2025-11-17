package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_categories_slug_type", columnList = "slug,category_type", unique = true),
        Index(name = "idx_categories_type", columnList = "category_type"),
        Index(name = "idx_categories_type_count", columnList = "category_type,repositories_count")
    ]
)
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val slug: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "category_type", nullable = false)
    val categoryType: String, // problem_domain, architecture_pattern, maturity_level, etc.

    @Column(name = "repositories_count")
    val repositoriesCount: Int = 0,

    @Column(columnDefinition = "JSON")
    val embedding: String? = null, // JSON string for vector embeddings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val repositoryCategories: List<RepositoryCategory> = emptyList(),

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val comparisonCategories: List<ComparisonCategory> = emptyList()
)
