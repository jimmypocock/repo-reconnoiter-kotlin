package com.reconnoiter.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * Spring Boot Application Context Test
 *
 * Verifies the application starts successfully and all beans load correctly.
 * Uses Testcontainers to provide a real MySQL database for Flyway migrations.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RepoReconnoiterApplicationTests {

	@Test
	fun contextLoads() {
		// Spring context loads successfully with all beans configured
	}

}
