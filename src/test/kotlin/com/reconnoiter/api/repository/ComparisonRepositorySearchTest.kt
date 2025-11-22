package com.reconnoiter.api.repository

import com.reconnoiter.api.TestcontainersConfiguration
import com.reconnoiter.api.entity.Comparison
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager
import java.time.LocalDateTime

/**
 * ComparisonRepository Search Integration Tests
 *
 * Tests FULLTEXT search with MySQL BOOLEAN MODE, synonym expansion,
 * SQL injection protection, and multi-field search.
 *
 * Uses Testcontainers to automatically start a real MySQL 8.0 container.
 * Tests FULLTEXT indexes accurately with n-gram parser (matches production).
 *
 * IMPORTANT: MySQL InnoDB FULLTEXT indexes require transactions to be COMMITTED
 * before data becomes searchable. We use TransactionTemplate to explicitly commit
 * test data, then perform searches outside of transactions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComparisonRepositorySearchTest {

    @Autowired
    private lateinit var comparisonRepository: ComparisonRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)

        // Clean up before each test (in a committed transaction)
        transactionTemplate.execute {
            comparisonRepository.deleteAll()
            entityManager.flush()
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test (in a committed transaction)
        transactionTemplate.execute {
            comparisonRepository.deleteAll()
            entityManager.flush()
        }
    }

    //--------------------------------------
    // MULTI-FIELD SEARCH
    //--------------------------------------

    @Test
    fun `advancedSearch finds by user_query`() {
        val comparison = createComparison("Rails background job library")

        val results = comparisonRepository.advancedSearch(
            searchTerms = "background*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == comparison.id },
            "Should find comparison by user_query (found ${results.content.size} results, looking for ID ${comparison.id})")
    }

    @Test
    fun `advancedSearch finds by technologies`() {
        val comparison = createComparison(
            userQuery = "job library",
            technologies = "Rails, Ruby"
        )

        val results = comparisonRepository.advancedSearch(
            searchTerms = "ruby*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == comparison.id },
            "Should find comparison by technologies")
    }

    @Test
    fun `advancedSearch finds by problem_domains`() {
        val comparison = createComparison(
            userQuery = "job library",
            problemDomains = "Background Job Processing"
        )

        val results = comparisonRepository.advancedSearch(
            searchTerms = "processing*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == comparison.id },
            "Should find comparison by problem_domains")
    }

    //--------------------------------------
    // CASE INSENSITIVITY
    //--------------------------------------

    @Test
    fun `advancedSearch is case insensitive`() {
        val comparison = createComparison(
            userQuery = "Rails library",
            technologies = "Rails, Ruby"
        )

        val searchTerms = listOf("RAILS*", "rails*", "RaIlS*")

        searchTerms.forEach { term ->
            val results = comparisonRepository.advancedSearch(
                searchTerms = term,
                startDate = null,
                endDate = null,
                pageable = PageRequest.of(0, 10)
            )

            assertTrue(results.content.any { it.id == comparison.id },
                "Should find comparison with search term: $term")
        }
    }

    //--------------------------------------
    // PARTIAL MATCHING (WILDCARD)
    //--------------------------------------

    @Test
    fun `advancedSearch handles partial matches with wildcard`() {
        val comparison = createComparison(
            userQuery = "authentication library",
            problemDomains = "Authentication"
        )

        // Wildcard search: "auth*" should match "authentication"
        val results = comparisonRepository.advancedSearch(
            searchTerms = "auth*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == comparison.id },
            "Should find comparison with partial match")
    }

    //--------------------------------------
    // MULTI-FIELD SEARCH
    //--------------------------------------

    @Test
    fun `advancedSearch finds across multiple fields`() {
        val comparison = createComparison(
            userQuery = "best job library",
            technologies = "Rails, Ruby",
            problemDomains = "Background Job Processing"
        )

        // Test searching each field independently
        val searchTerms = listOf("job*", "rails*", "background*", "processing*")

        searchTerms.forEach { term ->
            val results = comparisonRepository.advancedSearch(
                searchTerms = term,
                startDate = null,
                endDate = null,
                pageable = PageRequest.of(0, 10)
            )

            assertTrue(results.content.any { it.id == comparison.id },
                "Should find via search term: $term")
        }
    }

    @Test
    fun `advancedSearch does not match unrelated comparisons`() {
        val railsComparison = createComparison(
            userQuery = "Rails background job library",
            technologies = "Rails, Ruby",
            problemDomains = "Background Jobs"
        )

        val pythonComparison = createComparison(
            userQuery = "Python machine learning library",
            technologies = "Python",
            problemDomains = "Machine Learning"
        )

        val results = comparisonRepository.advancedSearch(
            searchTerms = "rails*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == railsComparison.id },
            "Should include Rails comparison")
        assertFalse(results.content.any { it.id == pythonComparison.id },
            "Should not include Python comparison")
    }

    //--------------------------------------
    // MULTI-WORD QUERIES
    //--------------------------------------

    @Test
    fun `advancedSearch handles multi-word queries with BOOLEAN MODE`() {
        val comparison = createComparison(
            userQuery = "Ruby on Rails background job library",
            technologies = "Rails, Ruby",
            problemDomains = "Background Job Processing"
        )

        // BOOLEAN MODE OR logic: "rails* background*"
        val results = comparisonRepository.advancedSearch(
            searchTerms = "rails* background*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == comparison.id },
            "Should find comparison with multi-word query")
    }

    //--------------------------------------
    // SECURITY: SQL INJECTION PROTECTION
    //--------------------------------------

    @Test
    fun `advancedSearch protects against SQL injection attempts`() {
        val comparison = createComparison(
            userQuery = "Rails library",
            technologies = "Rails, Ruby"
        )

        // These malicious inputs should not execute SQL, just be treated as search terms
        val maliciousInputs = listOf(
            "'; DROP TABLE comparisons; --",
            "' OR 1=1 --",
            "' UNION SELECT * FROM users --",
            "\\' OR \\'1\\'=\\'1",
            "admin'--",
            "1' AND '1' = '1",
            "'; DELETE FROM comparisons WHERE '1'='1",
            "' OR 'x'='x"
        )

        maliciousInputs.forEach { maliciousInput ->
            // Should not raise any errors when processing malicious input
            val results = comparisonRepository.advancedSearch(
                searchTerms = maliciousInput,
                startDate = null,
                endDate = null,
                pageable = PageRequest.of(0, 10)
            )

            // Should return a valid Page (not execute SQL)
            assertNotNull(results, "Should return valid Page for: $maliciousInput")

            // Should not delete our test comparison (proves DELETE didn't execute)
            assertTrue(comparisonRepository.existsById(comparison.id!!),
                "Comparison should still exist after: $maliciousInput")

            // Table should still exist and be queryable (proves DROP didn't execute)
            assertTrue(comparisonRepository.count() >= 1,
                "Table should still exist with at least 1 record")
        }
    }

    //--------------------------------------
    // BOOLEAN MODE SANITIZATION
    //--------------------------------------

    @Test
    fun `advancedSearch handles BOOLEAN MODE special characters safely`() {
        val comparison = createComparison("Rails background jobs")
        comparisonRepository.save(comparison)

        // These should be handled gracefully (sanitized by SearchSynonymExpander)
        // But if they make it through, parameter binding should still protect us
        val specialCharQueries = listOf(
            "+rails*",           // Required term
            "-django*",          // Excluded term
            "rails* -django*",   // Mix of required/excluded
            "\"rails jobs\"*",   // Phrase search
            "(rails | ruby)*"    // Grouping
        )

        specialCharQueries.forEach { query ->
            // Should not crash or execute malicious SQL
            val results = comparisonRepository.advancedSearch(
                searchTerms = query,
                startDate = null,
                endDate = null,
                pageable = PageRequest.of(0, 10)
            )

            assertNotNull(results, "Should handle query gracefully: $query")
        }
    }

    //--------------------------------------
    // DATE FILTERING
    //--------------------------------------

    @Test
    fun `advancedSearch filters by start date`() {
        val recentComparison = createComparison(
            userQuery = "Recent comparison",
            createdAt = LocalDateTime.now().minusDays(2)
        )

        val oldComparison = createComparison(
            userQuery = "Old comparison",
            createdAt = LocalDateTime.now().minusDays(10)
        )

        val results = comparisonRepository.advancedSearch(
            searchTerms = "comparison*",
            startDate = LocalDateTime.now().minusDays(7),  // Only last 7 days
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertTrue(results.content.any { it.id == recentComparison.id },
            "Should include recent comparison")
        assertFalse(results.content.any { it.id == oldComparison.id },
            "Should not include old comparison")
    }

    //--------------------------------------
    // PAGINATION
    //--------------------------------------

    @Test
    fun `advancedSearch respects pagination`() {
        // Create 5 comparisons
        repeat(5) { i ->
            val comparison = createComparison("Test comparison $i")
            comparisonRepository.save(comparison)
        }

        // Request page 0 with 2 items per page
        val results = comparisonRepository.advancedSearch(
            searchTerms = "test*",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 2)
        )

        assertEquals(2, results.content.size, "Should return 2 items per page")
        assertTrue(results.totalElements >= 5, "Should have at least 5 total items")
    }

    //--------------------------------------
    // BLANK/NULL HANDLING
    //--------------------------------------

    @Test
    fun `advancedSearch handles empty search terms`() {
        val comparison = createComparison("Test comparison")
        comparisonRepository.save(comparison)

        // Empty search should not crash
        val results = comparisonRepository.advancedSearch(
            searchTerms = "",
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        assertNotNull(results, "Should handle empty search gracefully")
    }

    //--------------------------------------
    // HELPER METHODS
    //--------------------------------------

    private fun createComparison(
        userQuery: String,
        technologies: String? = null,
        problemDomains: String? = null,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Comparison {
        // IMPORTANT: Must commit transaction for FULLTEXT index to see the data
        // InnoDB FULLTEXT indexes only index committed data
        //
        // IMPORTANT: FULLTEXT MATCH() AGAINST() across multiple columns requires
        // ALL columns to be non-NULL. Use empty strings instead of NULL.
        // See: https://stackoverflow.com/questions/43371210/
        return transactionTemplate.execute { status ->
            val comparison = comparisonRepository.save(
                Comparison(
                    userQuery = userQuery,
                    normalizedQuery = "",  // Empty string, not NULL (required for FULLTEXT)
                    technologies = technologies ?: "",  // Empty string, not NULL
                    problemDomains = problemDomains ?: "",  // Empty string, not NULL
                    architecturePatterns = "",  // Empty string, not NULL (required for FULLTEXT)
                    reposComparedCount = 3
                )
            )

            // Flush to get the ID
            entityManager.flush()

            // @CreationTimestamp overrides our createdAt, so manually update it if not current time
            if (createdAt != LocalDateTime.now()) {
                entityManager.createNativeQuery(
                    "UPDATE comparisons SET created_at = :createdAt WHERE id = :id"
                ).setParameter("createdAt", createdAt)
                 .setParameter("id", comparison.id)
                 .executeUpdate()
                entityManager.flush()
            }

            comparison
        }!!  // TransactionTemplate returns nullable, but we know it won't be null
    }
}
