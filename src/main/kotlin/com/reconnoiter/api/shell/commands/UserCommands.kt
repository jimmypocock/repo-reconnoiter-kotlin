package com.reconnoiter.api.shell.commands

import com.reconnoiter.api.repository.UserRepository
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.time.format.DateTimeFormatter

@ShellComponent
class UserCommands(private val userRepository: UserRepository) {

    @ShellMethod(key = ["user list", "users"], value = "List all users")
    fun listUsers(): String {
        val users = userRepository.findAll()

        if (users.isEmpty()) {
            return "No users found."
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("👥 Users (${users.size})")
        output.appendLine("━".repeat(100))
        output.appendLine(String.format("%-5s %-30s %-20s %-15s %-20s",
            "ID", "Email", "GitHub Username", "GitHub ID", "Created"))
        output.appendLine("━".repeat(100))

        users.forEach { user ->
            val created = user.createdAt?.format(formatter) ?: "N/A"
            output.appendLine(String.format("%-5d %-30s %-20s %-15s %-20s",
                user.id ?: 0,
                user.email?.take(30) ?: "N/A",
                user.githubUsername?.take(20) ?: "N/A",
                user.githubId ?: 0,
                created
            ))
        }

        output.appendLine("━".repeat(100))
        return output.toString()
    }

    @ShellMethod(key = ["user find"], value = "Find user by email")
    fun findByEmail(@ShellOption email: String): String {
        val user = userRepository.findByEmail(email)
            ?: return "❌ User not found: $email"

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("👤 User Details")
        output.appendLine("━".repeat(60))
        output.appendLine("ID:              ${user.id}")
        output.appendLine("Email:           ${user.email}")
        output.appendLine("GitHub ID:       ${user.githubId}")
        output.appendLine("GitHub Username: ${user.githubUsername}")
        output.appendLine("Created:         ${user.createdAt?.format(formatter) ?: "N/A"}")
        output.appendLine("Updated:         ${user.updatedAt?.format(formatter) ?: "N/A"}")
        output.appendLine("━".repeat(60))

        return output.toString()
    }

    @ShellMethod(key = ["user count"], value = "Count total users")
    fun countUsers(): String {
        val count = userRepository.count()
        return "Total users: $count"
    }
}
