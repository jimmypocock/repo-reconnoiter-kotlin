package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.service.ApiKeyService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@Component
@Profile("apiKeyList")
class ApiKeyListRunner(private val apiKeyService: ApiKeyService) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val apiKeys = apiKeyService.listActiveKeys()

            if (apiKeys.isEmpty()) {
                println("\nNo API keys found.")
                println("\nGenerate a new key with:")
                println("  ./gradlew apiKeyGenerate -Pname=\"Key Name\"")
                println()
                exitProcess(0)
            }

            println("\n📋 Active API Keys (${apiKeys.size})")
            println("━".repeat(120))
            println(String.format("%-5s %-20s %-12s %-30s %-12s %-20s",
                "ID", "Name", "Prefix", "User", "Requests", "Last Used"))
            println("━".repeat(120))

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            apiKeys.forEach { key ->
                val userName = key.user?.email ?: "System-wide"
                val lastUsed = key.lastUsedAt?.format(formatter) ?: "Never"

                println(String.format("%-5d %-20s %-12s %-30s %-12d %-20s",
                    key.id,
                    key.name.take(20),
                    key.prefix,
                    userName.take(30),
                    key.requestCount,
                    lastUsed
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
