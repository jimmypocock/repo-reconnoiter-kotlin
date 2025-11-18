package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "analysis_statuses",
    indexes = [
        Index(name = "idx_analysis_statuses_session", columnList = "session_id", unique = true),
        Index(name = "idx_analysis_statuses_user", columnList = "user_id"),
        Index(name = "idx_analysis_statuses_repository", columnList = "repository_id"),
        Index(name = "idx_analysis_statuses_status_created", columnList = "status,created_at")
    ]
)
data class AnalysisStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "session_id")
    val sessionId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    val repository: Repository? = null,

    @Column(name = "status")
    val status: String? = null, // processing, completed, failed

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "pending_cost_usd", nullable = false, precision = 10, scale = 6)
    val pendingCostUsd: BigDecimal = BigDecimal.ZERO,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
