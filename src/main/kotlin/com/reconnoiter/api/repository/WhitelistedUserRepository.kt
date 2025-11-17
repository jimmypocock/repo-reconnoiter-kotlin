package com.reconnoiter.api.repository

import com.reconnoiter.api.model.WhitelistedUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface WhitelistedUserRepository : JpaRepository<WhitelistedUser, Long> {
    fun existsByGithubId(githubId: Long): Boolean
    fun findByGithubId(githubId: Long): WhitelistedUser?
}
