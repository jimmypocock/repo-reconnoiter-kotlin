package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.RepositoryCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RepositoryCategoryRepository : JpaRepository<RepositoryCategory, Long>
