package com.reconnoiter.api.shell.commands

import com.reconnoiter.api.service.WhitelistService
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.time.format.DateTimeFormatter

@ShellComponent
class WhitelistCommands(
    private val whitelistService: WhitelistService
) {

    @ShellMethod(key = ["whitelist list", "whitelisted"], value = "List all whitelisted users")
    fun listWhitelistedUsers(): String {
        val users = whitelistService.listAll()

        if (users.isEmpty()) {
            return "\nNo whitelisted users found.\n\nAdd a user with: whitelist add <githubId> <username>"
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("👥 Whitelisted Users (${users.size})")
        output.appendLine("━".repeat(120))
        output.appendLine(String.format("%-5s %-15s %-12s %-30s %-30s %-20s",
            "ID", "Username", "GitHub ID", "Email", "Notes", "Added"))
        output.appendLine("━".repeat(120))

        users.forEach { user ->
            val email = user.email ?: "N/A"
            val notes = user.notes?.take(30) ?: "N/A"
            val added = user.createdAt?.format(formatter) ?: "N/A"

            output.appendLine(String.format("%-5d %-15s %-12d %-30s %-30s %-20s",
                user.id ?: 0,
                user.githubUsername.take(15),
                user.githubId,
                email.take(30),
                notes,
                added
            ))
        }

        output.appendLine("━".repeat(120))
        return output.toString()
    }

    @ShellMethod(key = ["whitelist add"], value = "Add a user to the whitelist")
    fun addUser(
        @ShellOption githubId: Long,
        @ShellOption username: String,
        @ShellOption(defaultValue = ShellOption.NULL) email: String?,
        @ShellOption(defaultValue = ShellOption.NULL) notes: String?
    ): String {
        return try {
            val user = whitelistService.addUser(githubId, username, email, notes)

            val output = StringBuilder()
            output.appendLine()
            output.appendLine("✅ User added to whitelist successfully!")
            output.appendLine("━".repeat(80))
            output.appendLine("GitHub ID:      ${user.githubId}")
            output.appendLine("Username:       ${user.githubUsername}")
            output.appendLine("Email:          ${user.email ?: "N/A"}")
            output.appendLine("Notes:          ${user.notes ?: "N/A"}")
            output.appendLine("Created:        ${user.createdAt}")
            output.appendLine("━".repeat(80))
            output.appendLine()
            output.appendLine("This user can now authenticate via GitHub OAuth.")

            output.toString()
        } catch (e: WhitelistService.AlreadyWhitelistedException) {
            "\n⚠️  ${e.message}\n"
        }
    }

    @ShellMethod(key = ["whitelist remove"], value = "Remove a user from the whitelist")
    fun removeUser(@ShellOption username: String): String {
        return try {
            val user = whitelistService.removeUser(username)

            val output = StringBuilder()
            output.appendLine()
            output.appendLine("✅ User removed from whitelist successfully!")
            output.appendLine("━".repeat(80))
            output.appendLine("GitHub ID:      ${user.githubId}")
            output.appendLine("Username:       ${user.githubUsername}")
            output.appendLine("Email:          ${user.email ?: "N/A"}")
            output.appendLine("━".repeat(80))
            output.appendLine()
            output.appendLine("This user can no longer authenticate via GitHub OAuth.")

            output.toString()
        } catch (e: WhitelistService.UserNotFoundException) {
            "\n❌ ${e.message}\n"
        }
    }

    @ShellMethod(key = ["whitelist check"], value = "Check if a GitHub ID is whitelisted")
    fun checkUser(@ShellOption githubId: Long): String {
        val isWhitelisted = whitelistService.isWhitelisted(githubId)
        val user = if (isWhitelisted) whitelistService.findByGithubId(githubId) else null

        return if (isWhitelisted && user != null) {
            val output = StringBuilder()
            output.appendLine()
            output.appendLine("✅ User is whitelisted")
            output.appendLine("━".repeat(80))
            output.appendLine("GitHub ID:      ${user.githubId}")
            output.appendLine("Username:       ${user.githubUsername}")
            output.appendLine("Email:          ${user.email ?: "N/A"}")
            output.appendLine("━".repeat(80))
            output.toString()
        } else {
            "\n❌ GitHub ID $githubId is NOT whitelisted\n"
        }
    }
}
