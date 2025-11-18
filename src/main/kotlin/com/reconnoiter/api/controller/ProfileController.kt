package com.reconnoiter.api.controller

import com.reconnoiter.api.entity.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class ProfileController {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal user: User?): ResponseEntity<ProfileResponse> {
        if (user == null) {
            return ResponseEntity.status(401).body(
                ProfileResponse(
                    id = null,
                    email = null,
                    githubUsername = null,
                    githubName = null,
                    githubAvatarUrl = null,
                    admin = false,
                    error = "Unauthorized - Please provide a valid X-User-Token header"
                )
            )
        }

        return ResponseEntity.ok(
            ProfileResponse(
                id = user.id,
                email = user.email,
                githubUsername = user.githubUsername,
                githubName = user.githubName,
                githubAvatarUrl = user.githubAvatarUrl,
                admin = user.admin
            )
        )
    }
}

data class ProfileResponse(
    val id: Long?,
    val email: String?,
    val githubUsername: String?,
    val githubName: String?,
    val githubAvatarUrl: String?,
    val admin: Boolean,
    val error: String? = null
)
