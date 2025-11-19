package com.reconnoiter.api.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import com.reconnoiter.api.entity.ApiKey
import com.reconnoiter.api.entity.User
import com.reconnoiter.api.repository.ApiKeyRepository

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository
) {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private const val API_KEY_LENGTH = 32
        private const val PREFIX_LENGTH = 8
        private val ENCODER = BCryptPasswordEncoder()
        private val SECURE_RANDOM = SecureRandom()
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    /**
     * Clean up old revoked keys (older than 90 days)
     */
    @Transactional
    fun cleanupOldRevokedKeys(daysOld: Int = 90): Int {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        val oldKeys = apiKeyRepository.findByRevokedAtBefore(cutoffDate)
        apiKeyRepository.deleteAll(oldKeys)
        return oldKeys.size
    }

    /**
     * Generate a new API key for a user or system-wide
     * Returns a pair of (raw key, ApiKey entity)
     * Raw key is shown only once and must be saved by the caller
     */
    @Transactional
    fun generateApiKey(name: String, user: User? = null): Pair<String, ApiKey> {
        // Generate random API key
        val rawKey = generateSecureKey()
        val prefix = rawKey.substring(0, PREFIX_LENGTH)
        val keyDigest = ENCODER.encode(rawKey)

        // Create API key entity
        val apiKey = ApiKey(
            name = name,
            keyDigest = keyDigest,
            prefix = prefix,
            user = user
        )

        val savedKey = apiKeyRepository.save(apiKey)
        return Pair(rawKey, savedKey)
    }

    /**
     * Get API key statistics
     * Returns a map with total, active, and revoked counts
     */
    fun getStats(): Map<String, Long> {
        val total = apiKeyRepository.count()
        val active = apiKeyRepository.findByRevokedAtIsNull().size.toLong()
        val revoked = total - active

        return mapOf(
            "total" to total,
            "active" to active,
            "revoked" to revoked
        )
    }

    /**
     * List all active API keys
     */
    fun listActiveKeys(): List<ApiKey> {
        return apiKeyRepository.findByRevokedAtIsNull()
    }

    /**
     * List all API keys for a specific user
     */
    fun listUserKeys(userId: Long, includeRevoked: Boolean = false): List<ApiKey> {
        return if (includeRevoked) {
            apiKeyRepository.findByUserId(userId)
        } else {
            apiKeyRepository.findByUserIdAndRevokedAtIsNull(userId)
        }
    }

    /**
     * Revoke an API key by ID
     */
    @Transactional
    fun revokeApiKey(id: Long): Boolean {
        val apiKey = apiKeyRepository.findById(id).orElse(null) ?: return false

        if (apiKey.revokedAt != null) {
            return false // Already revoked
        }

        val revoked = apiKey.copy(revokedAt = LocalDateTime.now())
        apiKeyRepository.save(revoked)
        return true
    }

    /**
     * Validate an API key against stored hash
     * Returns the ApiKey entity if valid, null otherwise
     * Updates last_used_at and request_count on successful validation
     */
    @Transactional
    fun validateApiKey(rawKey: String): ApiKey? {
        if (rawKey.length != API_KEY_LENGTH) {
            return null
        }

        val prefix = rawKey.substring(0, PREFIX_LENGTH)

        // Find all active keys with this prefix
        val candidates = apiKeyRepository.findByPrefixAndRevokedAtIsNull(prefix)

        // Check each candidate with BCrypt
        for (candidate in candidates) {
            if (ENCODER.matches(rawKey, candidate.keyDigest)) {
                // Update usage statistics
                val updated = candidate.copy(
                    requestCount = candidate.requestCount + 1,
                    lastUsedAt = LocalDateTime.now()
                )
                return apiKeyRepository.save(updated)
            }
        }

        return null
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

    private fun generateSecureKey(): String {
        val bytes = ByteArray(API_KEY_LENGTH)
        SECURE_RANDOM.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, API_KEY_LENGTH)
    }
}
