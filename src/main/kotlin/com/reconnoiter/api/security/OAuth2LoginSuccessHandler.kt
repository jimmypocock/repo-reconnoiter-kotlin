package com.reconnoiter.api.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

import com.reconnoiter.api.entity.User
import com.reconnoiter.api.repository.UserRepository
import com.reconnoiter.api.repository.WhitelistedUserRepository

@Component
class OAuth2LoginSuccessHandler(
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository,
    private val whitelistedUserRepository: WhitelistedUserRepository,
    @Value("\${app.frontend.url:http://localhost:3000}") private val frontendUrl: String
) : SimpleUrlAuthenticationSuccessHandler() {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private val LOG = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User

        // Extract GitHub user info
        val githubId = oauth2User.getAttribute<Int>("id")?.toLong()
        val email = oauth2User.getAttribute<String>("email") ?: ""
        val githubUsername = oauth2User.getAttribute<String>("login") ?: ""
        val githubName = oauth2User.getAttribute<String>("name")
        val githubAvatarUrl = oauth2User.getAttribute<String>("avatar_url")

        // Check whitelist BEFORE creating user
        if (githubId == null || !whitelistedUserRepository.existsByGithubId(githubId)) {
            LOG.warn("OAuth2 login attempt by non-whitelisted user: githubId=$githubId, username=$githubUsername")
            val errorUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/error")
                .queryParam("error", "not_whitelisted")
                .queryParam("message", "Your GitHub account is not authorized to access this application")
                .build()
                .toUriString()
            response.sendRedirect(errorUrl)
            return
        }

        // Find or create user (user is guaranteed to be whitelisted at this point)
        val user = userRepository.findByGithubId(githubId)
            ?: userRepository.findByEmail(email)
            ?: createUser(githubId, email, githubUsername, githubName, githubAvatarUrl)

        // Generate JWT token
        val token = jwtUtil.generateToken(user.id!!, user.email)

        // Redirect to frontend with token
        val targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/callback")
            .queryParam("token", token)
            .build()
            .toUriString()

        LOG.info("OAuth2 login successful for user: ${user.email}")
        response.sendRedirect(targetUrl)
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

    private fun createUser(
        githubId: Long,
        email: String,
        githubUsername: String,
        githubName: String?,
        githubAvatarUrl: String?
    ): User {
        val newUser = User(
            admin = false,
            email = email,
            githubAvatarUrl = githubAvatarUrl,
            githubId = githubId,
            githubName = githubName,
            githubUsername = githubUsername,
            provider = "github",
            uid = githubId.toString()
        )
        return userRepository.save(newUser)
    }
}
