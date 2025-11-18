package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "queued_analyses",
    indexes = [
        Index(name = "idx_queued_analyses_repository", columnList = "repository_id"),
        Index(name = "idx_queued_analyses_created_at", columnList = "created_at"),
        Index(name = "idx_queued_analyses_processing", columnList = "status,priority,scheduled_for")
    ]
)
data class QueuedAnalysis(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @Column(name = "analysis_type", nullable = false)
    val analysisType: String, // tier1_categorization, tier2_deep_analysis

    @Column(name = "status")
    val status: String = "pending", // pending, processing, completed, failed

    @Column(name = "priority")
    val priority: Int = 0,

    @Column(name = "retry_count")
    val retryCount: Int = 0,

    @Column(name = "scheduled_for")
    val scheduledFor: LocalDateTime? = null,

    @Column(name = "processed_at")
    val processedAt: LocalDateTime? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
