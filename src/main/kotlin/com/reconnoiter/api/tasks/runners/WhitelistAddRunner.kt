package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.entity.WhitelistedUser
import com.reconnoiter.api.repository.WhitelistedUserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("whitelistAdd")
class WhitelistAddRunner(private val whitelistedUserRepository: WhitelistedUserRepository) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val githubIdStr = args.getOrNull(0)
                ?: throw IllegalArgumentException("GitHub ID is required. Usage: ./gradlew whitelistAdd -PgithubId=123 -Pusername=octocat -Pemail=user@example.com")

            val githubId = githubIdStr.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid GitHub ID: $githubIdStr (must be a number)")

            val githubUsername = args.getOrNull(1)
                ?: throw IllegalArgumentException("GitHub username is required")

            val email = args.getOrNull(2)
            val notes = args.getOrNull(3)

            // Check if already whitelisted
            if (whitelistedUserRepository.existsByGithubId(githubId)) {
                println("\n⚠️  User with GitHub ID $githubId is already whitelisted.")
                println()
                exitProcess(0)
            }

            val whitelistedUser = WhitelistedUser(
                githubId = githubId,
                githubUsername = githubUsername,
                email = email,
                notes = notes
            )

            val saved = whitelistedUserRepository.save(whitelistedUser)

            println("\n✅ User added to whitelist successfully!")
            println("━".repeat(80))
            println("GitHub ID:      $githubId")
            println("Username:       $githubUsername")
            println("Email:          ${email ?: "N/A"}")
            println("Notes:          ${notes ?: "N/A"}")
            println("Created:        ${saved.createdAt}")
            println("━".repeat(80))
            println("\nThis user can now authenticate via GitHub OAuth.")
            println()

            exitProcess(0)
        } catch (e: IllegalArgumentException) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        } catch (e: Exception) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        }
    }
}
