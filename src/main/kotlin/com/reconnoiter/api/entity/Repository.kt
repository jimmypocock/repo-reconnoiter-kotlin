package com.reconnoiter.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "repositories",
    indexes = [
        Index(name = "idx_github_id", columnList = "github_id", unique = true),
        Index(name = "idx_node_id", columnList = "node_id", unique = true),
        Index(name = "idx_full_name", columnList = "full_name", unique = true),
        Index(name = "idx_stargazers_count", columnList = "stargazers_count"),
        Index(name = "idx_language", columnList = "language"),
        Index(name = "idx_github_created_at", columnList = "github_created_at"),
        Index(name = "idx_github_pushed_at", columnList = "github_pushed_at"),
        Index(name = "idx_last_analyzed_at", columnList = "last_analyzed_at"),
        Index(name = "idx_archived_disabled", columnList = "archived,disabled")
    ]
)
data class Repository(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, name = "github_id")
    val githubId: Long,

    @Column(nullable = false, unique = true, name = "node_id")
    val nodeId: String,

    @Column(nullable = false, unique = true, name = "full_name")
    val fullName: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, name = "html_url")
    val htmlUrl: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "owner_login")
    val ownerLogin: String? = null,

    @Column(name = "owner_avatar_url")
    val ownerAvatarUrl: String? = null,

    @Column(name = "owner_type")
    val ownerType: String? = null,

    val language: String? = null,

    @Column(name = "stargazers_count", nullable = false)
    val stargazersCount: Int = 0,

    @Column(name = "forks_count", nullable = false)
    val forksCount: Int = 0,

    @Column(name = "watchers_count", nullable = false)
    val watchersCount: Int = 0,

    @Column(name = "open_issues_count", nullable = false)
    val openIssuesCount: Int = 0,

    @Column(columnDefinition = "JSON", name = "topics")
    val topics: String = "[]",

    @Column(name = "homepage_url")
    val homepageUrl: String? = null,

    val license: String? = null,

    @Column(name = "default_branch", nullable = false)
    val defaultBranch: String = "main",

    @Column(name = "is_fork", nullable = false)
    val isFork: Boolean = false,

    @Column(name = "is_template", nullable = false)
    val isTemplate: Boolean = false,

    @Column(nullable = false)
    val archived: Boolean = false,

    @Column(nullable = false)
    val disabled: Boolean = false,

    @Column(nullable = false)
    val visibility: String = "public",

    @Column(name = "clone_url")
    val cloneUrl: String? = null,

    val size: Int? = null,

    @Column(name = "search_score")
    val searchScore: Double? = null,

    @Column(name = "github_created_at")
    val githubCreatedAt: LocalDateTime? = null,

    @Column(name = "github_updated_at")
    val githubUpdatedAt: LocalDateTime? = null,

    @Column(name = "github_pushed_at")
    val githubPushedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT", name = "readme_content")
    val readmeContent: String? = null,

    @Column(name = "readme_sha")
    val readmeSha: String? = null,

    @Column(name = "readme_length")
    val readmeLength: Int? = null,

    @Column(name = "readme_fetched_at")
    val readmeFetchedAt: LocalDateTime? = null,

    @Column(name = "last_analyzed_at")
    val lastAnalyzedAt: LocalDateTime? = null,

    @Column(name = "last_fetched_at")
    val lastFetchedAt: LocalDateTime? = null,

    @Column(name = "fetch_count", nullable = false)
    val fetchCount: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
