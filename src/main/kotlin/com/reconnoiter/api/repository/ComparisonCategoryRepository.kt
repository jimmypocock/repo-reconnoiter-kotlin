package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.ComparisonCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ComparisonCategoryRepository : JpaRepository<ComparisonCategory, Long>
