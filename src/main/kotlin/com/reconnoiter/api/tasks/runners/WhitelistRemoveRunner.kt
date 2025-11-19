package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.service.WhitelistService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("whitelistRemove")
class WhitelistRemoveRunner(private val whitelistService: WhitelistService) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val githubUsername = args.getOrNull(0)
                ?: throw IllegalArgumentException("GitHub username is required. Usage: ./gradlew whitelistRemove -Pusername=octocat")

            val user = whitelistService.removeUser(githubUsername)

            println("\n✅ User removed from whitelist successfully!")
            println("━".repeat(80))
            println("GitHub ID:      ${user.githubId}")
            println("Username:       ${user.githubUsername}")
            println("Email:          ${user.email ?: "N/A"}")
            println("━".repeat(80))
            println("\nThis user can no longer authenticate via GitHub OAuth.")
            println()

            exitProcess(0)
        } catch (e: WhitelistService.UserNotFoundException) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        } catch (e: IllegalArgumentException) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        } catch (e: Exception) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        }
    }
}
