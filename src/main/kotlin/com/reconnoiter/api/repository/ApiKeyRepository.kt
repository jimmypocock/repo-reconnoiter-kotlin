package com.reconnoiter.api.repository

import com.reconnoiter.api.model.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {

    /**
     * Find an active (non-revoked) API key by its prefix
     * Used for quick lookup before validating the full key
     */
    fun findByPrefixAndRevokedAtIsNull(prefix: String): List<ApiKey>

    /**
     * Find all active API keys for a specific user
     */
    fun findByUserIdAndRevokedAtIsNull(userId: Long): List<ApiKey>

    /**
     * Find all API keys (including revoked) for a specific user
     */
    fun findByUserId(userId: Long): List<ApiKey>

    /**
     * Find all active API keys
     */
    fun findByRevokedAtIsNull(): List<ApiKey>

    /**
     * Find API keys revoked before a certain date (for cleanup)
     */
    fun findByRevokedAtBefore(date: LocalDateTime): List<ApiKey>
}
