package com.reconnoiter.api.repository

import com.reconnoiter.api.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByGithubId(githubId: Long): User?
    fun findByProviderAndUid(provider: String, uid: String): User?
}
