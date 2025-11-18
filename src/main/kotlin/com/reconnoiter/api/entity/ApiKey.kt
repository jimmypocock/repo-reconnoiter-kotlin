package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "api_keys",
    indexes = [
        Index(name = "idx_api_keys_digest", columnList = "key_digest", unique = true),
        Index(name = "idx_api_keys_prefix", columnList = "prefix"),
        Index(name = "idx_api_keys_user", columnList = "user_id"),
        Index(name = "idx_api_keys_revoked", columnList = "revoked_at"),
        Index(name = "idx_api_keys_user_revoked", columnList = "user_id,revoked_at")
    ]
)
data class ApiKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String, // Friendly name (e.g., "Insomnia Testing")

    @Column(name = "key_digest", nullable = false)
    val keyDigest: String, // BCrypt hash of the API key

    @Column(name = "prefix")
    val prefix: String? = null, // First 8 characters for identification

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "request_count", nullable = false)
    val requestCount: Int = 0,

    @Column(name = "last_used_at")
    val lastUsedAt: LocalDateTime? = null,

    @Column(name = "revoked_at")
    val revokedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
