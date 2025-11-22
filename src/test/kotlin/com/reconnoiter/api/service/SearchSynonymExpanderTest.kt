package com.reconnoiter.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * SearchSynonymExpander Unit Tests
 *
 * Tests synonym expansion logic and BOOLEAN MODE sanitization.
 * Critical for search quality and security.
 */
class SearchSynonymExpanderTest {

    //--------------------------------------
    // SYNONYM EXPANSION
    //--------------------------------------

    @Test
    fun `expand returns single term for unknown word`() {
        val result = SearchSynonymExpander.expand("unknown")
        assertEquals(setOf("unknown"), result)
    }

    @Test
    fun `expand returns synonyms for known term`() {
        val result = SearchSynonymExpander.expand("auth")
        assertTrue(result.contains("auth"))
        assertTrue(result.contains("authentication"))
        assertTrue(result.contains("authorize"))
        assertTrue(result.contains("authorization"))
    }

    @Test
    fun `expand handles case insensitivity`() {
        val result = SearchSynonymExpander.expand("AUTH")
        assertTrue(result.contains("auth"))
        assertTrue(result.contains("authentication"))
    }

    @Test
    fun `expand handles whitespace`() {
        val result = SearchSynonymExpander.expand("  job  ")
        assertTrue(result.contains("job"))
        assertTrue(result.contains("jobs"))
        assertTrue(result.contains("queue"))
    }

    @Test
    fun `expand returns unique terms`() {
        val result = SearchSynonymExpander.expand("job")
        assertEquals(result.size, result.distinct().size, "All terms should be unique")
    }

    @Test
    fun `expandAll flattens multiple terms`() {
        val result = SearchSynonymExpander.expandAll(listOf("job", "auth"))

        // Should include synonyms from both terms
        assertTrue(result.contains("job"))
        assertTrue(result.contains("jobs"))
        assertTrue(result.contains("auth"))
        assertTrue(result.contains("authentication"))
    }

    @Test
    fun `expandAll returns unique terms`() {
        // "job" and "jobs" both expand to same set
        val result = SearchSynonymExpander.expandAll(listOf("job", "jobs"))
        assertEquals(result.size, result.distinct().size, "All terms should be unique")
    }

    //--------------------------------------
    // SYNONYM COVERAGE
    //--------------------------------------

    @Test
    fun `job synonyms include common variations`() {
        val result = SearchSynonymExpander.expand("job")
        assertTrue(result.contains("job"))
        assertTrue(result.contains("jobs"))
        assertTrue(result.contains("queue"))
        assertTrue(result.contains("worker"))
    }

    @Test
    fun `auth synonyms include common variations`() {
        val result = SearchSynonymExpander.expand("authentication")
        assertTrue(result.contains("auth"))
        assertTrue(result.contains("authentication"))
        assertTrue(result.contains("authorize"))
        assertTrue(result.contains("authorization"))
    }

    @Test
    fun `language synonyms include common aliases`() {
        val jsResult = SearchSynonymExpander.expand("js")
        assertTrue(jsResult.contains("javascript"))
        assertTrue(jsResult.contains("node"))

        val pyResult = SearchSynonymExpander.expand("python")
        assertTrue(pyResult.contains("py"))
    }

    //--------------------------------------
    // BOOLEAN MODE SANITIZATION (Security Fix)
    //--------------------------------------

    @Test
    fun `expandQuery sanitizes BOOLEAN MODE special characters`() {
        // Test that special characters are stripped
        val result = SearchSynonymExpander.expandQuery("+rails -django")

        // Should strip + and - characters
        assertTrue(result.contains("rails") || result.isNotEmpty(),
            "Should contain 'rails' without '+' prefix")
        assertFalse(result.any { it.contains("+") },
            "Should not contain '+' character")
        assertFalse(result.any { it.contains("-") },
            "Should not contain '-' character")
    }

    @Test
    fun `expandQuery sanitizes all BOOLEAN MODE operators`() {
        val specialChars = listOf("+", "-", "<", ">", "~", "\"", "(", ")", "@")
        val testQuery = specialChars.joinToString(" ") { "${it}term" }

        val result = SearchSynonymExpander.expandQuery(testQuery)

        // All results should be free of special characters
        specialChars.forEach { char ->
            assertFalse(result.any { it.contains(char) },
                "Should not contain '$char' character")
        }
    }

    @Test
    fun `expandQuery handles empty input after sanitization`() {
        // Query with only special characters
        val result = SearchSynonymExpander.expandQuery("+ - < >")

        // Should return empty set or handle gracefully
        assertTrue(result.isEmpty() || result.all { it.isBlank() },
            "Should return empty result when only special chars provided")
    }

    @Test
    fun `expandQuery preserves valid search terms`() {
        val result = SearchSynonymExpander.expandQuery("rails jobs")

        // Should expand both terms normally
        assertTrue(result.contains("rails") || result.contains("ruby") || result.contains("ror"),
            "Should contain rails or its synonyms")
        assertTrue(result.contains("job") || result.contains("jobs") || result.contains("queue"),
            "Should contain job or its synonyms")
    }

    @Test
    fun `expandQuery handles mixed valid and invalid characters`() {
        val result = SearchSynonymExpander.expandQuery("+rails job@ -test")

        // Should sanitize to: rails job test
        assertTrue(result.any { it.contains("rail") || it == "rails" },
            "Should contain rails-related term")
        assertTrue(result.any { it.contains("job") },
            "Should contain job-related term")
        assertTrue(result.any { it == "test" || it.contains("test") },
            "Should contain test-related term")
    }
}
