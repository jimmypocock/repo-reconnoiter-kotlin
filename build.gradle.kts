plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
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
