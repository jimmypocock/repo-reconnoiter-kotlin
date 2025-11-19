package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.service.WhitelistService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@Component
@Profile("whitelistList")
class WhitelistListRunner(private val whitelistService: WhitelistService) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val users = whitelistService.listAll()

            if (users.isEmpty()) {
                println("\nNo whitelisted users found.")
                println("\nAdd a user with:")
                println("  ./gradlew whitelistAdd -PgithubId=123 -Pusername=octocat -Pemail=user@example.com")
                println()
                exitProcess(0)
            }

            println("\n👥 Whitelisted Users (${users.size})")
            println("━".repeat(120))
            println(String.format("%-5s %-15s %-12s %-30s %-30s %-20s",
                "ID", "Username", "GitHub ID", "Email", "Notes", "Added"))
            println("━".repeat(120))

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            users.forEach { user ->
                val email = user.email ?: "N/A"
                val notes = user.notes?.take(30) ?: "N/A"
                val added = user.createdAt?.format(formatter) ?: "N/A"

                println(String.format("%-5d %-15s %-12d %-30s %-30s %-20s",
                    user.id ?: 0,
                    user.githubUsername.take(15),
                    user.githubId,
                    email.take(30),
                    notes,
                    added
                ))
            }
            println("━".repeat(120))
            println()

            exitProcess(0)
        } catch (e: Exception) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        }
    }
}
