package com.reconnoiter.api.repository

import com.reconnoiter.api.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findBySlug(slug: String): Category?
    fun findByCategoryType(categoryType: String): List<Category>
    fun findBySlugAndCategoryType(slug: String, categoryType: String): Category?
    fun existsBySlugAndCategoryType(slug: String, categoryType: String): Boolean
}
