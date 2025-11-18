package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.repository.UserRepository
import com.reconnoiter.api.service.ApiKeyService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("apiKeyGenerate")
class ApiKeyGenerateRunner(
    private val apiKeyService: ApiKeyService,
    private val userRepository: UserRepository
) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val keyName = args.getOrNull(0) ?: "default"
            val userEmail = args.getOrNull(1)

            val user = if (userEmail != null) {
                userRepository.findByEmail(userEmail)
                    ?: throw IllegalArgumentException("User with email '$userEmail' not found")
            } else {
                null
            }

            val (rawKey, apiKey) = apiKeyService.generateApiKey(keyName, user)

            println("\n✅ API Key Generated Successfully!")
            println("━".repeat(80))
            println("Name:           ${apiKey.name}")
            println("Key:            $rawKey")
            println("Prefix:         ${apiKey.prefix}")
            println("User:           ${user?.email ?: "System-wide (no user)"}")
            println("Created:        ${apiKey.createdAt}")
            println("━".repeat(80))
            println("\n⚠️  IMPORTANT: Save this key now - it will not be shown again!")
            println("\nUse in API requests:")
            println("Authorization: Bearer $rawKey")
            println()

            exitProcess(0)
        } catch (e: Exception) {
            System.err.println("\n❌ Error: ${e.message}")
            exitProcess(1)
        }
    }
}
