package com.reconnoiter.api.shell.commands

import com.reconnoiter.api.repository.UserRepository
import com.reconnoiter.api.service.ApiKeyService
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.time.format.DateTimeFormatter

@ShellComponent
class ApiKeyCommands(
    private val apiKeyService: ApiKeyService,
    private val userRepository: UserRepository
) {

    @ShellMethod(key = ["apikey list", "apikeys"], value = "List all active API keys")
    fun listApiKeys(): String {
        val apiKeys = apiKeyService.listActiveKeys()

        if (apiKeys.isEmpty()) {
            return "\nNo active API keys found.\n\nGenerate a new key with: apikey generate <name>"
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val output = StringBuilder()

        output.appendLine()
        output.appendLine("🔑 Active API Keys (${apiKeys.size})")
        output.appendLine("━".repeat(120))
        output.appendLine(String.format("%-5s %-20s %-12s %-30s %-12s %-20s",
            "ID", "Name", "Prefix", "User", "Requests", "Last Used"))
        output.appendLine("━".repeat(120))

        apiKeys.forEach { key ->
            val userName = key.user?.email ?: "System-wide"
            val lastUsed = key.lastUsedAt?.format(formatter) ?: "Never"

            output.appendLine(String.format("%-5d %-20s %-12s %-30s %-12d %-20s",
                key.id,
                key.name.take(20),
                key.prefix,
                userName.take(30),
                key.requestCount,
                lastUsed
            ))
        }

        output.appendLine("━".repeat(120))
        return output.toString()
    }

    @ShellMethod(key = ["apikey generate"], value = "Generate a new API key")
    fun generateApiKey(
        @ShellOption name: String,
        @ShellOption(defaultValue = ShellOption.NULL) email: String?
    ): String {
        val user = if (email != null) {
            userRepository.findByEmail(email)
                ?: return "❌ User with email '$email' not found"
        } else {
            null
        }

        val (rawKey, apiKey) = apiKeyService.generateApiKey(name, user)

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("✅ API Key Generated Successfully!")
        output.appendLine("━".repeat(80))
        output.appendLine("Name:           ${apiKey.name}")
        output.appendLine("Key:            $rawKey")
        output.appendLine("Prefix:         ${apiKey.prefix}")
        output.appendLine("User:           ${user?.email ?: "System-wide (no user)"}")
        output.appendLine("Created:        ${apiKey.createdAt}")
        output.appendLine("━".repeat(80))
        output.appendLine()
        output.appendLine("⚠️  IMPORTANT: Save this key now - it will not be shown again!")
        output.appendLine()
        output.appendLine("Use in API requests:")
        output.appendLine("Authorization: Bearer $rawKey")

        return output.toString()
    }

    @ShellMethod(key = ["apikey revoke"], value = "Revoke an API key by ID")
    fun revokeApiKey(@ShellOption id: Long): String {
        val revoked = apiKeyService.revokeApiKey(id)

        return if (revoked) {
            "\n✅ API Key #$id revoked successfully!\n\nThe key can no longer be used for authentication."
        } else {
            "\n⚠️  API Key #$id not found or already revoked."
        }
    }

    @ShellMethod(key = ["apikey stats"], value = "Show API key usage statistics")
    fun showStats(): String {
        val stats = apiKeyService.getStats()

        val output = StringBuilder()
        output.appendLine()
        output.appendLine("📊 API Key Statistics")
        output.appendLine("━".repeat(40))
        output.appendLine("Total keys:    ${stats["total"]}")
        output.appendLine("Active keys:   ${stats["active"]}")
        output.appendLine("Revoked keys:  ${stats["revoked"]}")
        output.appendLine("━".repeat(40))

        return output.toString()
    }
}
