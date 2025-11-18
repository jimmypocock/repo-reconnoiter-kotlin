package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.service.ApiKeyService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("apiKeyRevoke")
class ApiKeyRevokeRunner(private val apiKeyService: ApiKeyService) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            val idStr = args.getOrNull(0)
                ?: throw IllegalArgumentException("API Key ID is required. Usage: ./gradlew apiKeyRevoke -Pid=123")

            val id = idStr.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid ID: $idStr (must be a number)")

            val revoked = apiKeyService.revokeApiKey(id)

            if (revoked) {
                println("\n✅ API Key #$id revoked successfully!")
                println("\nThe key can no longer be used for authentication.")
                println()
            } else {
                println("\n⚠️  API Key #$id not found or already revoked.")
                println("\nList active keys with:")
                println("  ./gradlew apiKeyList")
                println()
            }

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
