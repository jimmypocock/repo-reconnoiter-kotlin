package com.reconnoiter.api.service

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with GitHub API
 * Uses WebClient with timeouts for production-ready HTTP calls
 */
@Service
class GitHubService(
    @Value("\${github.api.base-url:https://api.github.com}") private val githubApiUrl: String
) {

    private val webClient: WebClient

    init {
        // Configure HTTP client with timeouts
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5 second connection timeout
            .responseTimeout(Duration.ofSeconds(10)) // 10 second response timeout
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(10, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
            }

        webClient = WebClient.builder()
            .baseUrl(githubApiUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .build()
    }

    /**
     * Fetch GitHub user data using OAuth token
     * @param token GitHub OAuth token
     * @return GitHubUserData or null if invalid/forbidden token
     * @throws Exception for system errors (5xx, timeouts) - Sentry will catch these in production
     */
    fun fetchUser(token: String): GitHubUserData? {
        return try {
            val response = webClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GitHubUserResponse::class.java)
                .block() // Block for synchronous behavior (AuthService expects sync)

            response?.let {
                GitHubUserData(
                    id = it.id,
                    login = it.login,
                    email = it.email,
                    avatarUrl = it.avatar_url,
                    name = it.name
                )
            }
        } catch (e: WebClientResponseException) {
            // Expected user errors - return null, AuthService handles the message
            when (e.statusCode) {
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> null
                else -> throw e // System errors (5xx, rate limits) - let Sentry catch them
            }
        }
        // Don't catch other exceptions - let timeouts, network errors bubble up to Sentry
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
