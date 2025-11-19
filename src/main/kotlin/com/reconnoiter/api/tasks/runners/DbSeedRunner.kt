package com.reconnoiter.api.tasks.runners

import com.reconnoiter.api.service.DatabaseSeeder
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("dbSeed")
class DbSeedRunner(private val databaseSeeder: DatabaseSeeder) : CommandLineRunner {

    override fun run(vararg args: String) {
        try {
            println("\n🌱 Seeding database...")
            println("━".repeat(80))

            val (createdCount, skippedCount) = databaseSeeder.seedCategories()

            println("━".repeat(80))
            println("✅ Seeding complete!")
            println("   Created: $createdCount categories")
            println("   Skipped: $skippedCount categories (already existed)")
            println()

            exitProcess(0)
        } catch (e: Exception) {
            System.err.println("\n❌ Error: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
