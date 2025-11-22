package com.reconnoiter.api

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers Configuration for Integration Tests
 *
 * Automatically starts a MySQL Docker container before tests and stops it after.
 * Uses Spring Boot 3.1+ @ServiceConnection to automatically configure datasource properties.
 *
 * Benefits:
 * - Zero manual setup - just run `./gradlew test`
 * - Works in local dev and CI/CD without changes
 * - Automatically starts/stops containers
 * - Uses real MySQL for accurate FULLTEXT index testing
 *
 * Usage: Add @Import(TestcontainersConfiguration::class) to test classes
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> {
        return MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .apply {
                withDatabaseName("reconnoiter_test")
                withUsername("reconnoiter")
                withPassword("testpassword")
                // Enable FULLTEXT n-gram parser (matches production config)
                withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-authentication-plugin=mysql_native_password"
                )
                // Reuse containers across test classes for faster execution
                withReuse(true)
            }
    }
}
