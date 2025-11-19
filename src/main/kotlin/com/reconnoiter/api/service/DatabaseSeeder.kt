package com.reconnoiter.api.service

import com.reconnoiter.api.entity.Category
import com.reconnoiter.api.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DatabaseSeeder(
    private val categoryRepository: CategoryRepository
) {

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    /**
     * Get all category definitions (for reporting what would be seeded)
     */
    fun getCategoryDefinitions(): List<Category> {
        return listOf(
            // Problem Domains
            Category(name = "Web Development", slug = "web-development", categoryType = "problem_domain",
                description = "Tools and frameworks for building web applications"),
            Category(name = "Data Science", slug = "data-science", categoryType = "problem_domain",
                description = "Libraries and tools for data analysis and machine learning"),
            Category(name = "DevOps", slug = "devops", categoryType = "problem_domain",
                description = "Infrastructure, deployment, and operations tools"),
            Category(name = "Mobile Development", slug = "mobile-development", categoryType = "problem_domain",
                description = "Frameworks for building mobile applications"),
            Category(name = "Testing", slug = "testing", categoryType = "problem_domain",
                description = "Testing frameworks and tools"),
            Category(name = "Authentication", slug = "authentication", categoryType = "problem_domain",
                description = "User authentication and authorization"),
            Category(name = "API Development", slug = "api-development", categoryType = "problem_domain",
                description = "Tools for building REST and GraphQL APIs"),

            // Architecture Patterns
            Category(name = "MVC Framework", slug = "mvc-framework", categoryType = "architecture_pattern",
                description = "Model-View-Controller architecture"),
            Category(name = "Microservices", slug = "microservices", categoryType = "architecture_pattern",
                description = "Microservices architecture pattern"),
            Category(name = "Serverless", slug = "serverless", categoryType = "architecture_pattern",
                description = "Serverless computing pattern"),
            Category(name = "Event-Driven", slug = "event-driven", categoryType = "architecture_pattern",
                description = "Event-driven architecture"),

            // Maturity Levels
            Category(name = "Production Ready", slug = "production-ready", categoryType = "maturity_level",
                description = "Stable and widely used in production"),
            Category(name = "Active Development", slug = "active-development", categoryType = "maturity_level",
                description = "Under active development"),
            Category(name = "Experimental", slug = "experimental", categoryType = "maturity_level",
                description = "Early stage or experimental")
        )
    }

    /**
     * Seed the database with initial categories
     * Returns a pair of (created count, skipped count)
     */
    @Transactional
    fun seedCategories(): Pair<Int, Int> {
        var createdCount = 0
        var skippedCount = 0

        val categories = listOf(
            // Problem Domains
            Category(
                name = "Web Development",
                slug = "web-development",
                categoryType = "problem_domain",
                description = "Tools and frameworks for building web applications"
            ),
            Category(
                name = "Data Science",
                slug = "data-science",
                categoryType = "problem_domain",
                description = "Libraries and tools for data analysis and machine learning"
            ),
            Category(
                name = "DevOps",
                slug = "devops",
                categoryType = "problem_domain",
                description = "Infrastructure, deployment, and operations tools"
            ),
            Category(
                name = "Mobile Development",
                slug = "mobile-development",
                categoryType = "problem_domain",
                description = "Frameworks for building mobile applications"
            ),
            Category(
                name = "Testing",
                slug = "testing",
                categoryType = "problem_domain",
                description = "Testing frameworks and tools"
            ),
            Category(
                name = "Authentication",
                slug = "authentication",
                categoryType = "problem_domain",
                description = "User authentication and authorization"
            ),
            Category(
                name = "API Development",
                slug = "api-development",
                categoryType = "problem_domain",
                description = "Tools for building REST and GraphQL APIs"
            ),

            // Architecture Patterns
            Category(
                name = "MVC Framework",
                slug = "mvc-framework",
                categoryType = "architecture_pattern",
                description = "Model-View-Controller architecture"
            ),
            Category(
                name = "Microservices",
                slug = "microservices",
                categoryType = "architecture_pattern",
                description = "Microservices architecture pattern"
            ),
            Category(
                name = "Serverless",
                slug = "serverless",
                categoryType = "architecture_pattern",
                description = "Serverless computing pattern"
            ),
            Category(
                name = "Event-Driven",
                slug = "event-driven",
                categoryType = "architecture_pattern",
                description = "Event-driven architecture"
            ),

            // Maturity Levels
            Category(
                name = "Production Ready",
                slug = "production-ready",
                categoryType = "maturity_level",
                description = "Stable and widely used in production"
            ),
            Category(
                name = "Active Development",
                slug = "active-development",
                categoryType = "maturity_level",
                description = "Under active development"
            ),
            Category(
                name = "Experimental",
                slug = "experimental",
                categoryType = "maturity_level",
                description = "Early stage or experimental"
            )
        )

        categories.forEach { category ->
            if (!categoryRepository.existsBySlugAndCategoryType(category.slug, category.categoryType)) {
                categoryRepository.save(category)
                createdCount++
            } else {
                skippedCount++
            }
        }

        return Pair(createdCount, skippedCount)
    }
}
