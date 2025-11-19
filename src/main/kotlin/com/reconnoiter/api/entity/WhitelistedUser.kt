package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "whitelisted_users",
    indexes = [
        Index(name = "idx_whitelisted_github_id", columnList = "github_id", unique = true)
    ]
)
data class WhitelistedUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "github_id", nullable = false, unique = true)
    val githubId: Long,

    @Column(name = "github_username", nullable = false)
    val githubUsername: String,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "notes")
    val notes: String? = null,

    @Column(name = "added_by")
    val addedBy: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
