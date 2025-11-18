package com.reconnoiter.api.shell.commands

import com.reconnoiter.api.repository.RepositoryRepository
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.time.format.DateTimeFormatter

@ShellComponent
class RepositoryCommands(private val repositoryRepository: RepositoryRepository) {

    @ShellMethod(key = ["repo list", "repos"], value = "List repositories")
    fun listRepositories(
        @ShellOption(defaultValue = "10") limit: Int
    ): String {
        val repos = repositoryRepository.findAll().take(limit)

        if (repos.isEmpty()) {
            return "No repositories found."
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("📦 Repositories (showing ${repos.size})")
        output.appendLine("━".repeat(120))
        output.appendLine(String.format("%-5s %-40s %-10s %-20s %-15s",
            "ID", "Full Name", "Stars", "Language", "Last Fetched"))
        output.appendLine("━".repeat(120))

        repos.forEach { repo ->
            val lastFetched = repo.lastFetchedAt?.format(formatter) ?: "Never"

            output.appendLine(String.format("%-5d %-40s %-10d %-20s %-15s",
                repo.id ?: 0,
                repo.fullName.take(40),
                repo.stargazersCount,
                repo.language?.take(20) ?: "N/A",
                lastFetched
            ))
        }

        output.appendLine("━".repeat(120))
        output.appendLine("\nShowing first $limit repositories. Use --limit to see more.")

        return output.toString()
    }

    @ShellMethod(key = ["repo search"], value = "Search repositories by name")
    fun searchRepositories(@ShellOption query: String): String {
        val repos = repositoryRepository.findAll()
            .filter { it.fullName.contains(query, ignoreCase = true) }
            .take(20)

        if (repos.isEmpty()) {
            return "No repositories found matching: $query"
        }

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("🔍 Search Results for '$query' (${repos.size})")
        output.appendLine("━".repeat(100))

        repos.forEach { repo ->
            output.appendLine("${repo.id}: ${repo.fullName} (⭐ ${repo.stargazersCount})")
            output.appendLine("   ${repo.description?.take(80) ?: "No description"}")
            output.appendLine()
        }

        output.appendLine("━".repeat(100))
        return output.toString()
    }

    @ShellMethod(key = ["repo show"], value = "Show repository details by ID")
    fun showRepository(@ShellOption id: Long): String {
        val repo = repositoryRepository.findById(id).orElse(null)
            ?: return "❌ Repository not found: $id"

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("📦 Repository Details")
        output.appendLine("━".repeat(80))
        output.appendLine("ID:              ${repo.id}")
        output.appendLine("Full Name:       ${repo.fullName}")
        output.appendLine("GitHub ID:       ${repo.githubId}")
        output.appendLine("Description:     ${repo.description ?: "N/A"}")
        output.appendLine("Language:        ${repo.language ?: "N/A"}")
        output.appendLine("Stars:           ${repo.stargazersCount}")
        output.appendLine("Forks:           ${repo.forksCount}")
        output.appendLine("Open Issues:     ${repo.openIssuesCount}")
        output.appendLine("Homepage:        ${repo.homepageUrl ?: "N/A"}")
        output.appendLine("Created:         ${repo.createdAt?.format(formatter) ?: "N/A"}")
        output.appendLine("Last Fetched:    ${repo.lastFetchedAt?.format(formatter) ?: "Never"}")
        output.appendLine("━".repeat(80))

        return output.toString()
    }

    @ShellMethod(key = ["repo count"], value = "Count total repositories")
    fun countRepositories(): String {
        val count = repositoryRepository.count()
        return "Total repositories: $count"
    }
}
