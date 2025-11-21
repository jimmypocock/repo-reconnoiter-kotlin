plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
	id("io.sentry.jvm.gradle") version "5.12.2"
	// Temporarily disabled - will re-add after fixing code quality
	// id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "com.reconnoiter"
version = "0.0.1-SNAPSHOT"
description = "GitHub repository analysis API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient for GitHub API
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Database migrations
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-mysql")

	// JWT dependencies (JJWT)
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Sentry error tracking (production only - disabled in dev)
	implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.22.0")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Generate build info for actuator /info endpoint
springBoot {
	buildInfo()
}

// Specify main class for bootJar (since we have multiple main classes in task scripts)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
}

// Custom tasks (like npm scripts)
tasks.register("version") {
	group = "help"
	description = "Display project Java version"
	doLast {
		val toolchain = project.extensions.getByType<JavaToolchainService>()
		val javaLauncher = toolchain.launcherFor {
			languageVersion.set(JavaLanguageVersion.of(21))
		}.get()

		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
		println("📦 Project Java Configuration")
		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
		println("Project compiles to: Java 21")
		println("Toolchain location: ${javaLauncher.metadata.installationPath}")
		println("Gradle daemon runs on: Java ${JavaVersion.current()}")
		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
	}
}

// Helper function to read .env file manually (fallback when plugin not available)
fun getEnvOrDefault(key: String, default: String = ""): String {
	val envFile = project.file(".env")
	if (envFile.exists()) {
		envFile.readLines().forEach { line ->
			val parts = line.split("=", limit = 2)
			if (parts.size == 2 && parts[0].trim() == key) {
				return parts[1].trim()
			}
		}
	}
	return default
}

// Configure bootRun to load environment variables from .env (local dev only)
// Skip during Docker builds (.env not available - env vars passed via docker-compose)
if (project.file(".env").exists()) {
	afterEvaluate {
		tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
			environment("SENTRY_DSN", getEnvOrDefault("SENTRY_DSN"))
			environment("DATABASE_URL", getEnvOrDefault("DATABASE_URL"))
			environment("DATABASE_USERNAME", getEnvOrDefault("DATABASE_USERNAME"))
			environment("DATABASE_PASSWORD", getEnvOrDefault("DATABASE_PASSWORD"))
			environment("GITHUB_CLIENT_ID", getEnvOrDefault("GITHUB_CLIENT_ID"))
			environment("GITHUB_CLIENT_SECRET", getEnvOrDefault("GITHUB_CLIENT_SECRET"))
			environment("JWT_SECRET", getEnvOrDefault("JWT_SECRET"))
			environment("APP_FRONTEND_URL", getEnvOrDefault("APP_FRONTEND_URL", "http://localhost:3000"))
		}

		// Configure all JavaExec tasks (shell, apiKeyList, etc.) to load environment variables
		tasks.withType<JavaExec>().configureEach {
			environment("SENTRY_DSN", getEnvOrDefault("SENTRY_DSN"))
			environment("DATABASE_URL", getEnvOrDefault("DATABASE_URL"))
			environment("DATABASE_USERNAME", getEnvOrDefault("DATABASE_USERNAME"))
			environment("DATABASE_PASSWORD", getEnvOrDefault("DATABASE_PASSWORD"))
			environment("GITHUB_CLIENT_ID", getEnvOrDefault("GITHUB_CLIENT_ID"))
			environment("GITHUB_CLIENT_SECRET", getEnvOrDefault("GITHUB_CLIENT_SECRET"))
			environment("JWT_SECRET", getEnvOrDefault("JWT_SECRET"))
			environment("APP_FRONTEND_URL", getEnvOrDefault("APP_FRONTEND_URL", "http://localhost:3000"))
		}
	}
}

tasks.register("dev") {
	group = "application"
	description = "Run the application in development mode"
	dependsOn("bootRun")
}

tasks.register("info") {
	group = "help"
	description = "Display project information"
	doLast {
		println("Project: ${project.name}")
		println("Version: ${project.version}")
		println("Description: ${project.description}")
		println("Java Toolchain: 21")
		println("Kotlin: 2.2.21")
		println("Spring Boot: 3.5.7")
		println("Gradle: 9.2.0")
	}
}

tasks.register("checkEnv") {
	group = "help"
	description = "Check if .env variables are loaded (local dev only)"
	doLast {
		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
		println("🔍 Environment Variable Check")
		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

		if (project.file(".env").exists()) {
			// Check DATABASE_URL
			val dbUrl = getEnvOrDefault("DATABASE_URL")
			if (dbUrl.isNotEmpty()) {
				println("✅ DATABASE_URL: Found")
			} else {
				println("❌ DATABASE_URL: Not found in .env")
			}

			// Check SENTRY_DSN
			val sentryDsn = getEnvOrDefault("SENTRY_DSN")
			if (sentryDsn.isNotEmpty()) {
				println("✅ SENTRY_DSN: Found")
			} else {
				println("ℹ️  SENTRY_DSN: Not set (optional in dev)")
			}
		} else {
			println("⚠️  .env not available (using system environment)")
		}

		println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
	}
}

// Database Tasks (like Rails rake tasks)
tasks.register<JavaExec>("dbSeed") {
	group = "database"
	description = "Seed the database with initial data"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,dbSeed")
}

// API Key Tasks (like Rails rake api_keys:*)
tasks.register<JavaExec>("apiKeyGenerate") {
	group = "api_keys"
	description = "Generate a new API key. Usage: ./gradlew apiKeyGenerate -Pname='Key Name' [-Pemail='user@example.com']"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,apiKeyGenerate")

	// Pass properties as args
	val keyName = project.findProperty("name")?.toString() ?: "default"
	val userEmail = project.findProperty("email")?.toString()
	args = if (userEmail != null) listOf(keyName, userEmail) else listOf(keyName)
}

tasks.register<JavaExec>("apiKeyList") {
	group = "api_keys"
	description = "List all API keys with usage stats"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,apiKeyList")
}

tasks.register<JavaExec>("apiKeyRevoke") {
	group = "api_keys"
	description = "Revoke an API key. Usage: ./gradlew apiKeyRevoke -Pid=123"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,apiKeyRevoke")

	doFirst {
		val keyId = project.findProperty("id")?.toString() ?: error("Please provide -Pid=<key_id>")
		args = listOf(keyId)
	}
}

// Whitelist Tasks (like Rails rake whitelist:*)
tasks.register<JavaExec>("whitelistAdd") {
	group = "whitelist"
	description = "Add user to whitelist. Usage: ./gradlew whitelistAdd -PgithubId=123 -Pusername='user' [-Pemail='user@example.com'] [-Pnotes='notes']"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,whitelistAdd")

	doFirst {
		val githubId = project.findProperty("githubId")?.toString() ?: error("Please provide -PgithubId=<id>")
		val username = project.findProperty("username")?.toString() ?: error("Please provide -Pusername=<username>")
		val email = project.findProperty("email")?.toString()
		val notes = project.findProperty("notes")?.toString()

		args = listOfNotNull(githubId, username, email, notes)
	}
}

tasks.register<JavaExec>("whitelistList") {
	group = "whitelist"
	description = "List all whitelisted users"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,whitelistList")
}

tasks.register<JavaExec>("whitelistRemove") {
	group = "whitelist"
	description = "Remove user from whitelist. Usage: ./gradlew whitelistRemove -Pusername='user'"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,whitelistRemove")

	doFirst {
		val username = project.findProperty("username")?.toString() ?: error("Please provide -Pusername=<username>")
		args = listOf(username)
	}
}

// Spring Shell (Interactive Console like Rails console)
tasks.register<JavaExec>("shell") {
	group = "application"
	description = "Launch interactive Spring Shell console (like Rails console)"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.reconnoiter.api.RepoReconnoiterApplicationKt")
	environment("SPRING_PROFILES_ACTIVE", "dev,console,shell")
	standardInput = System.`in`
}

// Sentry Gradle Plugin Configuration
// Uploads source code to Sentry for better stack traces (shows actual code in error reports)
// Configuration loaded from sentry.properties (auto-detected by plugin)
sentry {
	includeSourceContext.set(true)
	// org, projectName, and authToken are loaded from sentry.properties automatically

	// Reduce Sentry CLI verbosity (suppress warnings)
	debug.set(false)
}

// ktlint Configuration - DISABLED TEMPORARILY
// Will re-enable after fixing code quality issues
// configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
// 	version.set("1.1.1")
// 	android.set(false)
// 	verbose.set(true)
// 	outputToConsole.set(true)
// 	coloredOutput.set(true)
// 	ignoreFailures.set(true)
// 	enableExperimentalRules.set(false)
// 	filter {
// 		exclude("**/generated/**")
// 		exclude("**/build/**")
// 		exclude("**/*.kts")
// 	}
// }

// tasks.named("compileKotlin") {
// 	dependsOn("ktlintCheck")
// }

// tasks.register("formatImports") {
// 	group = "formatting"
// 	description = "Auto-fix import ordering using ktlint"
// 	dependsOn("ktlintFormat")
// }
