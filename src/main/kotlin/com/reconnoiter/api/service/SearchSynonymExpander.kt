package com.reconnoiter.api.service

import org.springframework.stereotype.Service

/**
 * Search Synonym Expander
 *
 * Expands search terms with synonyms to improve search recall.
 * Similar to Rails SearchSynonymExpander but adapted for MySQL FULLTEXT search.
 *
 * Usage:
 *   val terms = SearchSynonymExpander.expand("jobs")
 *   // Returns: ["job", "jobs", "queue", "worker", "background"]
 */
@Service
object SearchSynonymExpander {

    // MySQL BOOLEAN MODE special characters that need to be sanitized
    // These characters have special meaning in FULLTEXT BOOLEAN MODE searches
    private val BOOLEAN_MODE_SPECIAL_CHARS = Regex("[+\\-<>~\"()@]")

    private val SYNONYMS = mapOf(
        // Authentication & Authorization
        "auth" to listOf("auth", "authentication", "authorize", "authorization", "oauth", "sso", "login"),
        "authentication" to listOf("auth", "authentication", "authorize", "authorization", "oauth", "sso", "login"),

        // Background Jobs & Queues
        "job" to listOf("job", "jobs", "queue", "queues", "worker", "workers", "background", "async", "sidekiq"),
        "jobs" to listOf("job", "jobs", "queue", "queues", "worker", "workers", "background", "async", "sidekiq"),
        "queue" to listOf("job", "jobs", "queue", "queues", "worker", "workers", "background", "async"),
        "worker" to listOf("job", "jobs", "queue", "queues", "worker", "workers", "background", "async"),
        "background" to listOf("job", "jobs", "queue", "queues", "worker", "workers", "background", "async"),

        // Database & ORM
        "database" to listOf("database", "db", "sql", "postgres", "mysql", "mongodb", "orm"),
        "db" to listOf("database", "db", "sql", "postgres", "mysql", "mongodb", "orm"),
        "orm" to listOf("orm", "activerecord", "hibernate", "jpa", "sequelize", "prisma"),

        // Web Frameworks
        "framework" to listOf("framework", "rails", "django", "spring", "express", "laravel", "flask"),
        "rails" to listOf("rails", "ruby", "activerecord", "ror"),
        "django" to listOf("django", "python", "flask"),
        "spring" to listOf("spring", "springboot", "java", "kotlin"),

        // Programming Languages
        "js" to listOf("javascript", "js", "node", "nodejs", "typescript", "ts"),
        "javascript" to listOf("javascript", "js", "node", "nodejs", "typescript", "ts"),
        "node" to listOf("node", "nodejs", "javascript", "js"),
        "nodejs" to listOf("node", "nodejs", "javascript", "js"),
        "python" to listOf("python", "py", "django", "flask"),
        "ruby" to listOf("ruby", "rails", "ror"),

        // API & Web Services
        "api" to listOf("api", "rest", "graphql", "grpc", "http", "endpoint"),
        "rest" to listOf("rest", "api", "http", "endpoint", "restful"),
        "graphql" to listOf("graphql", "api", "gql"),

        // Testing
        "test" to listOf("test", "testing", "spec", "rspec", "jest", "junit", "pytest"),
        "testing" to listOf("test", "testing", "spec", "rspec", "jest", "junit", "pytest"),

        // Deployment & DevOps
        "deploy" to listOf("deploy", "deployment", "ci", "cd", "docker", "kubernetes", "devops"),
        "deployment" to listOf("deploy", "deployment", "ci", "cd", "docker", "kubernetes", "devops"),
        "docker" to listOf("docker", "container", "containerization", "k8s", "kubernetes"),

        // Caching
        "cache" to listOf("cache", "caching", "redis", "memcached", "cdn"),
        "caching" to listOf("cache", "caching", "redis", "memcached", "cdn"),
        "redis" to listOf("redis", "cache", "caching", "memcached"),

        // Monitoring & Logging
        "monitor" to listOf("monitor", "monitoring", "observability", "logging", "metrics", "apm"),
        "monitoring" to listOf("monitor", "monitoring", "observability", "logging", "metrics", "apm"),
        "log" to listOf("log", "logging", "logs", "sentry", "datadog", "newrelic"),
        "logging" to listOf("log", "logging", "logs", "sentry", "datadog", "newrelic")
    )

    /**
     * Sanitize a search term by removing MySQL BOOLEAN MODE special characters
     * @param term The search term to sanitize
     * @return Sanitized term with special characters removed
     */
    private fun sanitizeForBooleanMode(term: String): String {
        return term.replace(BOOLEAN_MODE_SPECIAL_CHARS, "")
    }

    /**
     * Expand a search term with its synonyms
     * @param term The search term to expand
     * @return List of the term and its synonyms (deduped)
     */
    fun expand(term: String): Set<String> {
        val lowercaseTerm = term.lowercase().trim()
        return SYNONYMS[lowercaseTerm]?.toSet() ?: setOf(lowercaseTerm)
    }

    /**
     * Expand multiple search terms
     * @param terms List of search terms
     * @return Set of all terms and their synonyms (deduped)
     */
    fun expandAll(terms: List<String>): Set<String> {
        return terms.flatMap { expand(it) }.toSet()
    }

    /**
     * Expand a search query string (splits on spaces)
     * @param query The search query string
     * @return Set of all terms and their synonyms (deduped)
     */
    fun expandQuery(query: String): Set<String> {
        val terms = query.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { sanitizeForBooleanMode(it) }  // Sanitize BOOLEAN MODE special characters
            .filter { it.isNotEmpty() }  // Re-filter in case sanitization left empty strings

        return expandAll(terms)
    }
}
