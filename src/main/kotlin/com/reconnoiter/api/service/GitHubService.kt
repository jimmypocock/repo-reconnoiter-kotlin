package com.reconnoiter.api.service

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException

/**
 * Service for interacting with GitHub API
 */
@Service
class GitHubService {

    private val restTemplate = RestTemplate()
    private val githubApiUrl = "https://api.github.com"

    /**
     * Fetch GitHub user data using OAuth token
     * @param token GitHub OAuth token
     * @return GitHubUserData or null if invalid token
     */
    fun fetchUser(token: String): GitHubUserData? {
        return try {
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $token")
                set("Accept", "application/vnd.github+json")
            }

            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(
                "$githubApiUrl/user",
                HttpMethod.GET,
                entity,
                GitHubUserResponse::class.java
            )

            response.body?.let {
                GitHubUserData(
                    id = it.id,
                    login = it.login,
                    email = it.email,
                    avatarUrl = it.avatar_url,
                    name = it.name
                )
            }
        } catch (e: HttpClientErrorException) {
            // Invalid token or other HTTP error
            null
        } catch (e: Exception) {
            // Network error or other issue
            null
        }
    }
}

/**
 * GitHub API user response
 */
data class GitHubUserResponse(
    val id: Long,
    val login: String,
    val email: String?,
    val avatar_url: String?,
    val name: String?
)

/**
 * Simplified GitHub user data
 */
data class GitHubUserData(
    val id: Long,
    val login: String,
    val email: String?,
    val avatarUrl: String?,
    val name: String?
)
