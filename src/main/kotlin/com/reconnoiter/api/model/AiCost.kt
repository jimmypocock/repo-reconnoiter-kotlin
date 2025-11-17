package com.reconnoiter.api.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "ai_costs",
    indexes = [
        Index(name = "idx_ai_costs_date_model", columnList = "date,model_used", unique = true),
        Index(name = "idx_ai_costs_date", columnList = "date"),
        Index(name = "idx_ai_costs_user", columnList = "user_id")
    ]
)
data class AiCost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "date", nullable = false)
    val date: LocalDate,

    @Column(name = "model_used", nullable = false)
    val modelUsed: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "total_cost_usd", precision = 10, scale = 6)
    val totalCostUsd: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_input_tokens")
    val totalInputTokens: Long = 0,

    @Column(name = "total_output_tokens")
    val totalOutputTokens: Long = 0,

    @Column(name = "total_requests")
    val totalRequests: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
