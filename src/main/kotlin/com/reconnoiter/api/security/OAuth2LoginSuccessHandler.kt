package com.reconnoiter.api.security

import com.reconnoiter.api.model.User
import com.reconnoiter.api.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2LoginSuccessHandler(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User

        // Extract GitHub user info
        val githubId = oauth2User.getAttribute<Int>("id")?.toLong()
        val email = oauth2User.getAttribute<String>("email") ?: ""
        val githubUsername = oauth2User.getAttribute<String>("login")
        val githubName = oauth2User.getAttribute<String>("name")
        val githubAvatarUrl = oauth2User.getAttribute<String>("avatar_url")

        // Find or create user
        val user = githubId?.let { userRepository.findByGithubId(it) }
            ?: userRepository.findByEmail(email)
            ?: createUser(githubId, email, githubUsername, githubName, githubAvatarUrl)

        // Generate JWT token
        val token = jwtUtil.generateToken(user.id!!, user.email)

        // Redirect to frontend with token (or return JSON in production)
        val targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
            .queryParam("token", token)
            .build()
            .toUriString()

        response.sendRedirect(targetUrl)
    }

    private fun createUser(
        githubId: Long?,
        email: String,
        githubUsername: String?,
        githubName: String?,
        githubAvatarUrl: String?
    ): User {
        val newUser = User(
            email = email,
            githubId = githubId,
            githubUsername = githubUsername,
            githubName = githubName,
            githubAvatarUrl = githubAvatarUrl,
            provider = "github",
            uid = githubId?.toString(),
            admin = false
        )
        return userRepository.save(newUser)
    }
}
