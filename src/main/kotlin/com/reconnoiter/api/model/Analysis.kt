package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "analyses",
    indexes = [
        Index(name = "idx_analyses_repository", columnList = "repository_id"),
        Index(name = "idx_analyses_user", columnList = "user_id"),
        Index(name = "idx_analyses_type", columnList = "type"),
        Index(name = "idx_analyses_is_current", columnList = "is_current"),
        Index(name = "idx_analyses_created_at", columnList = "created_at"),
        Index(name = "idx_analyses_cost", columnList = "cost_usd"),
        Index(name = "idx_analyses_current", columnList = "repository_id,type,is_current"),
        Index(name = "idx_analyses_user_created", columnList = "user_id,created_at"),
        Index(name = "idx_analyses_user_type_created", columnList = "user_id,type,created_at")
    ]
)
data class Analysis(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(nullable = false)
    val type: String, // tier1_categorization, tier2_deep_analysis

    @Column(columnDefinition = "TEXT")
    val summary: String? = null,

    @Column(name = "use_cases", columnDefinition = "TEXT")
    val useCases: String? = null,

    @Column(name = "why_care", columnDefinition = "TEXT")
    val whyCare: String? = null,

    @Column(name = "key_insights", columnDefinition = "TEXT")
    val keyInsights: String? = null,

    @Column(name = "readme_analysis", columnDefinition = "TEXT")
    val readmeAnalysis: String? = null,

    @Column(name = "issues_analysis", columnDefinition = "TEXT")
    val issuesAnalysis: String? = null,

    @Column(name = "adoption_analysis", columnDefinition = "TEXT")
    val adoptionAnalysis: String? = null,

    @Column(name = "maintenance_analysis", columnDefinition = "TEXT")
    val maintenanceAnalysis: String? = null,

    @Column(name = "security_analysis", columnDefinition = "TEXT")
    val securityAnalysis: String? = null,

    @Column(name = "learning_value", columnDefinition = "TEXT")
    val learningValue: String? = null,

    @Column(name = "maturity_assessment")
    val maturityAssessment: String? = null,

    @Column(name = "quality_signals", columnDefinition = "JSON")
    val qualitySignals: String? = null,

    @Column(name = "model_used", nullable = false)
    val modelUsed: String,

    @Column(name = "input_tokens")
    val inputTokens: Int? = null,

    @Column(name = "output_tokens")
    val outputTokens: Int? = null,

    @Column(name = "cost_usd", precision = 10, scale = 6)
    val costUsd: BigDecimal? = null,

    @Column(name = "is_current")
    val isCurrent: Boolean = true,

    @Column(name = "content_hash")
    val contentHash: String? = null,

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
