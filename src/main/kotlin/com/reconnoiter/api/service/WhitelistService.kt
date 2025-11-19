package com.reconnoiter.api.service

import com.reconnoiter.api.entity.WhitelistedUser
import com.reconnoiter.api.repository.WhitelistedUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WhitelistService(
    private val whitelistedUserRepository: WhitelistedUserRepository
) {

    //--------------------------------------
    // CUSTOM EXCEPTIONS
    //--------------------------------------

    class AlreadyWhitelistedException(message: String) : RuntimeException(message)
    class UserNotFoundException(message: String) : RuntimeException(message)

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    /**
     * Add a user to the whitelist
     * Throws AlreadyWhitelistedException if user is already whitelisted
     */
    @Transactional
    fun addUser(
        githubId: Long,
        githubUsername: String,
        email: String? = null,
        notes: String? = null
    ): WhitelistedUser {
        if (whitelistedUserRepository.existsByGithubId(githubId)) {
            throw AlreadyWhitelistedException("User with GitHub ID $githubId is already whitelisted")
        }

        val whitelistedUser = WhitelistedUser(
            githubId = githubId,
            githubUsername = githubUsername,
            email = email,
            notes = notes
        )

        return whitelistedUserRepository.save(whitelistedUser)
    }

    /**
     * Find user by GitHub ID
     */
    fun findByGithubId(githubId: Long): WhitelistedUser? {
        return whitelistedUserRepository.findByGithubId(githubId)
    }

    /**
     * Find user by GitHub username
     */
    fun findByGithubUsername(githubUsername: String): WhitelistedUser? {
        return whitelistedUserRepository.findByGithubUsername(githubUsername)
    }

    /**
     * Check if user is whitelisted by GitHub ID
     */
    fun isWhitelisted(githubId: Long): Boolean {
        return whitelistedUserRepository.existsByGithubId(githubId)
    }

    /**
     * List all whitelisted users (ordered by created date, newest first)
     */
    fun listAll(): List<WhitelistedUser> {
        return whitelistedUserRepository.findAllByOrderByCreatedAtDesc()
    }

    /**
     * Remove user from whitelist by GitHub username
     * Throws UserNotFoundException if user not found
     */
    @Transactional
    fun removeUser(githubUsername: String): WhitelistedUser {
        val user = whitelistedUserRepository.findByGithubUsername(githubUsername)
            ?: throw UserNotFoundException("User '$githubUsername' not found in whitelist")

        whitelistedUserRepository.delete(user)
        return user
    }
}
