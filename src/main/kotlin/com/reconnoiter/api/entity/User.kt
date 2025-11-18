package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_email", columnList = "email", unique = true),
        Index(name = "idx_github_id", columnList = "github_id", unique = true),
        Index(name = "idx_provider_uid", columnList = "provider,uid", unique = true)
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "github_id", unique = true)
    val githubId: Long? = null,

    @Column(name = "github_username")
    val githubUsername: String? = null,

    @Column(name = "github_name")
    val githubName: String? = null,

    @Column(name = "github_avatar_url")
    val githubAvatarUrl: String? = null,

    @Column
    val provider: String? = null,

    @Column
    val uid: String? = null,

    @Column(nullable = false)
    val admin: Boolean = false,

    @Column(name = "encrypted_password", nullable = false)
    val encryptedPassword: String = "",

    @Column(name = "reset_password_token")
    val resetPasswordToken: String? = null,

    @Column(name = "reset_password_sent_at")
    val resetPasswordSentAt: LocalDateTime? = null,

    @Column(name = "remember_created_at")
    val rememberCreatedAt: LocalDateTime? = null,

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,

    @Column(name = "whitelisted_user_id")
    val whitelistedUserId: Long? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
