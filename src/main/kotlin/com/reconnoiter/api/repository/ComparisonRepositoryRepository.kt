package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.ComparisonRepository as ComparisonRepositoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ComparisonRepositoryRepository : JpaRepository<ComparisonRepositoryEntity, Long>
