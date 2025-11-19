package com.reconnoiter.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.math.BigInteger

@Service
class DatabaseSeeder(
    private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper
) {

    // Track inserted IDs by natural keys for FK lookups
    private val whitelistedUserIds = mutableMapOf<Long, Long>() // github_id -> db_id
    private val userIds = mutableMapOf<String, Long>() // email -> db_id
    private val categoryIds = mutableMapOf<String, Long>() // slug -> db_id
    private val repositoryIds = mutableMapOf<String, Long>() // full_name -> db_id
    private val comparisonIds = mutableMapOf<String, Long>() // normalized_query -> db_id

    @Transactional
    fun seedAll(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()

        // Clear tracking maps for idempotency
        whitelistedUserIds.clear()
        userIds.clear()
        categoryIds.clear()
        repositoryIds.clear()
        comparisonIds.clear()

        // Load in dependency order
        stats["whitelisted_users"] = loadWhitelistedUsers()
        stats["users"] = loadUsers()
        stats["categories"] = loadCategories()
        stats["repositories"] = loadRepositories()
        stats["analyses"] = loadAnalyses()
        stats["api_keys"] = loadApiKeys()
        stats["repository_categories"] = loadRepositoryCategories()
        stats["comparisons"] = loadComparisons()
        stats["comparison_repositories"] = loadComparisonRepositories()
        stats["comparison_categories"] = loadComparisonCategories()

        val aiCostsResource = ClassPathResource("seeds/ai_costs.json")
        if (aiCostsResource.exists()) {
            stats["ai_costs"] = loadGenericTable("ai_costs", aiCostsResource)
        }

        return stats
    }

    private fun loadWhitelistedUsers(): Int {
        val resource = ClassPathResource("seeds/whitelisted_users.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading whitelisted_users.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            val githubId = (columns["github_id"] as Number).toLong()

            val id = insertAndGetId("whitelisted_users", columns)
            whitelistedUserIds[githubId] = id
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadUsers(): Int {
        val resource = ClassPathResource("seeds/users.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading users.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            val email = columns["email"] as String

            // FK lookup: whitelisted_user_github_id -> whitelisted_user_id
            if (columns.containsKey("whitelisted_user_github_id")) {
                val whitelistedGithubId = (columns.remove("whitelisted_user_github_id") as Number).toLong()
                columns["whitelisted_user_id"] = whitelistedUserIds[whitelistedGithubId]
                    ?: throw IllegalStateException("WhitelistedUser with github_id=$whitelistedGithubId not found")
            }

            val id = insertAndGetId("users", columns)
            userIds[email] = id
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadCategories(): Int {
        val resource = ClassPathResource("seeds/categories.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading categories.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            val slug = columns["slug"] as String

            val id = insertAndGetId("categories", columns)
            categoryIds[slug] = id
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadRepositories(): Int {
        val resource = ClassPathResource("seeds/repositories.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading repositories.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            val fullName = columns["full_name"] as String

            val id = insertAndGetId("repositories", columns)
            repositoryIds[fullName] = id
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadAnalyses(): Int {
        val resource = ClassPathResource("seeds/analyses.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading analyses.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()

            // FK lookup: repository_full_name -> repository_id
            if (columns.containsKey("repository_full_name")) {
                val repositoryFullName = columns.remove("repository_full_name") as String
                columns["repository_id"] = repositoryIds[repositoryFullName]
                    ?: throw IllegalStateException("Repository with full_name=$repositoryFullName not found")
            }

            insertAndGetId("analyses", columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadApiKeys(): Int {
        val resource = ClassPathResource("seeds/api_keys.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading api_keys.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()

            // FK lookup: user_email -> user_id
            if (columns.containsKey("user_email")) {
                val userEmail = columns.remove("user_email") as String
                columns["user_id"] = userIds[userEmail]
                    ?: throw IllegalStateException("User with email=$userEmail not found")
            }

            insertAndGetId("api_keys", columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadRepositoryCategories(): Int {
        val resource = ClassPathResource("seeds/repository_categories.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading repository_categories.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()

            // FK lookups
            val repositoryFullName = columns.remove("repository_full_name") as String
            val categorySlug = columns.remove("category_slug") as String

            columns["repository_id"] = repositoryIds[repositoryFullName]
                ?: throw IllegalStateException("Repository with full_name=$repositoryFullName not found")
            columns["category_id"] = categoryIds[categorySlug]
                ?: throw IllegalStateException("Category with slug=$categorySlug not found")

            insertAndGetId("repository_categories", columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadComparisons(): Int {
        val resource = ClassPathResource("seeds/comparisons.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading comparisons.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            val normalizedQuery = columns["normalized_query"] as String

            // FK lookup: user_email -> user_id
            if (columns.containsKey("user_email")) {
                val userEmail = columns.remove("user_email") as String
                columns["user_id"] = userIds[userEmail]
            }

            // recommended_repo_full_name stays as-is (stored as VARCHAR, not FK)

            // Set default status if not provided
            if (!columns.containsKey("status")) {
                columns["status"] = "completed"
            }

            val id = insertAndGetId("comparisons", columns)
            comparisonIds[normalizedQuery] = id
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadComparisonRepositories(): Int {
        val resource = ClassPathResource("seeds/comparison_repositories.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading comparison_repositories.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()

            // FK lookups
            val comparisonQuery = (columns.remove("comparison_user_query") as String).lowercase()
            val repositoryFullName = columns.remove("repository_full_name") as String

            columns["comparison_id"] = comparisonIds[comparisonQuery]
                ?: throw IllegalStateException("Comparison with query=$comparisonQuery not found")
            columns["repository_id"] = repositoryIds[repositoryFullName]
                ?: throw IllegalStateException("Repository with full_name=$repositoryFullName not found")

            insertAndGetId("comparison_repositories", columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadComparisonCategories(): Int {
        val resource = ClassPathResource("seeds/comparison_categories.json")
        if (!resource.exists()) return 0

        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading comparison_categories.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()

            // FK lookups
            val comparisonQuery = (columns.remove("comparison_user_query") as String).lowercase()
            val categorySlug = columns.remove("category_slug") as String

            columns["comparison_id"] = comparisonIds[comparisonQuery]
                ?: throw IllegalStateException("Comparison with query=$comparisonQuery not found")
            columns["category_id"] = categoryIds[categorySlug]
                ?: throw IllegalStateException("Category with slug=$categorySlug not found")

            insertAndGetId("comparison_categories", columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun loadGenericTable(tableName: String, resource: ClassPathResource): Int {
        val records: List<Map<String, Any?>> = objectMapper.readValue(resource.inputStream)
        var count = 0

        println("📦 Loading ${tableName}.json...")
        records.forEach { record ->
            val columns = record.toMutableMap()
            insertAndGetId(tableName, columns)
            count++
        }
        println("   ✅ Loaded $count records")
        return count
    }

    private fun insertAndGetId(tableName: String, columns: Map<String, Any?>): Long {
        val columnNames = columns.keys.joinToString(", ") { "`$it`" }
        val values = columns.values.joinToString(", ") { formatValue(it) }

        val sql = "INSERT INTO `$tableName` ($columnNames) VALUES ($values)"

        try {
            entityManager.createNativeQuery(sql).executeUpdate()

            // Get the last inserted ID - handle both BigInteger and Long
            val idResult = entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").singleResult
            return when (idResult) {
                is BigInteger -> idResult.toLong()
                is Long -> idResult
                is Number -> idResult.toLong()
                else -> throw IllegalStateException("Unexpected ID type: ${idResult::class}")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to insert into $tableName: ${e.message}\nSQL: $sql", e)
        }
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "NULL"
            is String -> {
                // Convert ISO 8601 datetime to MySQL format
                val converted = if (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z?"))) {
                    value.replace("T", " ").replace("Z", "")
                } else {
                    value
                }
                "'${converted.replace("'", "''")}'"
            }
            is Number -> value.toString()
            is Boolean -> if (value) "1" else "0"
            else -> "'${value.toString().replace("'", "''")}'"
        }
    }
}
