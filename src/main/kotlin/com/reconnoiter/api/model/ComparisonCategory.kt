package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "comparison_categories",
    indexes = [
        Index(name = "idx_comparison_categories_unique", columnList = "comparison_id,category_id", unique = true),
        Index(name = "idx_comparison_categories_comparison", columnList = "comparison_id"),
        Index(name = "idx_comparison_categories_category", columnList = "category_id")
    ]
)
data class ComparisonCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_id", nullable = false)
    val comparison: Comparison,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @Column(name = "confidence_score", precision = 3, scale = 2)
    val confidenceScore: BigDecimal? = null,

    @Column(name = "assigned_by")
    val assignedBy: String = "inferred",

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
