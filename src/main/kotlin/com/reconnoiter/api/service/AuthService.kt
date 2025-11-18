package com.reconnoiter.api.service

import com.reconnoiter.api.entity.User
import com.reconnoiter.api.repository.UserRepository
import com.reconnoiter.api.repository.WhitelistedUserRepository
import com.reconnoiter.api.security.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling authentication and user management
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val whitelistedUserRepository: WhitelistedUserRepository,
    private val githubService: GitHubService,
    private val jwtUtil: JwtUtil
) {

    /**
     * Exchange GitHub OAuth token for JWT
     * @param githubToken GitHub OAuth token
     * @return AuthExchangeResult containing JWT and user data, or error
     */
    @Transactional
    fun exchangeGitHubToken(githubToken: String): AuthExchangeResult {
        // Fetch GitHub user data
        val githubUser = githubService.fetchUser(githubToken)
            ?: return AuthExchangeResult.Error("Invalid GitHub token", "Could not verify GitHub token or fetch user data")

        // Check if user is whitelisted
        if (!whitelistedUserRepository.existsByGithubId(githubUser.id)) {
            return AuthExchangeResult.Error("Access denied", "Your GitHub account is not whitelisted for access")
        }

        // Find or create user
        val user = findOrCreateUser(githubUser)

        // Generate JWT
        val jwt = jwtUtil.generateToken(user.id!!, user.email)

        // Return success result
        return AuthExchangeResult.Success(
            jwt = jwt,
            user = UserData(
                id = user.id!!,
                githubId = user.githubId,
                githubUsername = user.githubUsername,
                email = user.email,
                avatarUrl = user.githubAvatarUrl,
                name = user.githubName,
                admin = user.admin
            )
        )
    }

    /**
     * Find or create user from GitHub data
     * Updates user data on every login
     */
    private fun findOrCreateUser(githubUser: GitHubUserData): User {
        val existingUser = userRepository.findByGithubId(githubUser.id)

        return if (existingUser != null) {
            // Update existing user data (GitHub data might have changed)
            existingUser.copy(
                githubUsername = githubUser.login,
                email = githubUser.email ?: "${githubUser.login}@users.noreply.github.com",
                githubAvatarUrl = githubUser.avatarUrl,
                githubName = githubUser.name
            ).let { userRepository.save(it) }
        } else {
            // Create new user
            val newUser = User(
                email = githubUser.email ?: "${githubUser.login}@users.noreply.github.com",
                githubId = githubUser.id,
                githubUsername = githubUser.login,
                githubName = githubUser.name,
                githubAvatarUrl = githubUser.avatarUrl,
                provider = "github",
                uid = githubUser.id.toString(),
                admin = false
            )
            userRepository.save(newUser)
        }
    }
}

/**
 * Result of auth exchange operation
 */
sealed class AuthExchangeResult {
    data class Success(val jwt: String, val user: UserData) : AuthExchangeResult()
    data class Error(val message: String, val details: String) : AuthExchangeResult()
}

/**
 * User data for API responses
 */
data class UserData(
    val id: Long,
    val githubId: Long?,
    val githubUsername: String?,
    val email: String,
    val avatarUrl: String?,
    val name: String?,
    val admin: Boolean
)
