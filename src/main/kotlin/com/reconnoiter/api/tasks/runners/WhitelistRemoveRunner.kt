package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.repository.WhitelistedUserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("whitelistRemove")
class WhitelistRemoveRunner(private val whitelistedUserRepository: WhitelistedUserRepository) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val githubUsername = args.getOrNull(0)
                ?: throw IllegalArgumentException("GitHub username is required. Usage: ./gradlew whitelistRemove -Pusername=octocat")

            val user = whitelistedUserRepository.findByGithubUsername(githubUsername)
                ?: throw IllegalArgumentException("User '$githubUsername' not found in whitelist")

            whitelistedUserRepository.delete(user)

            println("\n✅ User removed from whitelist successfully!")
            println("━".repeat(80))
            println("GitHub ID:      ${user.githubId}")
            println("Username:       ${user.githubUsername}")
            println("Email:          ${user.email ?: "N/A"}")
            println("━".repeat(80))
            println("\nThis user can no longer authenticate via GitHub OAuth.")
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
