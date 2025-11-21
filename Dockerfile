# ============================================
# Multi-Stage Dockerfile for Spring Boot 3.5
# Java 21 | Gradle | Production-Ready
# ADMIN VERSION: Includes Gradle for admin tasks
# ============================================

# ============================================
# Stage 1: Build Stage
# Uses full JDK to compile the application
# ============================================
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set working directory
WORKDIR /workspace/app

# Copy Gradle wrapper and configuration files first (for caching)
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Accept Sentry auth token as build arg (not committed to image)
ARG SENTRY_AUTH_TOKEN
RUN if [ -n "$SENTRY_AUTH_TOKEN" ]; then \
        echo "auth.token=${SENTRY_AUTH_TOKEN}" > sentry.properties; \
    else \
        echo "Warning: SENTRY_AUTH_TOKEN not provided - source uploads will be skipped" >&2; \
        touch sentry.properties; \
    fi

# Download dependencies (cached layer unless dependencies change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application (skip tests for faster builds - run tests in CI/CD)
RUN ./gradlew bootJar --no-daemon -x test

# Extract the built JAR layers for optimal caching
RUN mkdir -p build/dependency && \
    cd build/dependency && \
    jar -xf ../libs/*.jar

# ============================================
# Stage 2: Admin Runtime Stage
# Uses JDK (not JRE) + keeps Gradle for admin tasks
# ============================================
FROM eclipse-temurin:21-jdk-jammy

# Install utilities: dumb-init, curl, mysql-client
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        dumb-init \
        curl \
        mysql-client && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files (needed for Gradle tasks)
COPY --from=builder --chown=spring:spring /workspace/app/gradlew ./
COPY --from=builder --chown=spring:spring /workspace/app/gradle gradle/
COPY --from=builder --chown=spring:spring /workspace/app/gradle.properties ./
COPY --from=builder --chown=spring:spring /workspace/app/settings.gradle.kts ./
COPY --from=builder --chown=spring:spring /workspace/app/build.gradle.kts ./

# Copy source files (needed for Gradle tasks to work)
COPY --from=builder --chown=spring:spring /workspace/app/src src/

# Copy extracted layers from builder (for running the app)
ARG DEPENDENCY=/workspace/app/build/dependency
COPY --from=builder --chown=spring:spring ${DEPENDENCY}/BOOT-INF/lib lib/
COPY --from=builder --chown=spring:spring ${DEPENDENCY}/META-INF META-INF/
COPY --from=builder --chown=spring:spring ${DEPENDENCY}/BOOT-INF/classes classes/

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# Use dumb-init to handle signals properly (graceful shutdown)
ENTRYPOINT ["dumb-init", "--"]

# Run the application with optimized JVM settings
CMD ["java", \
     # Memory settings (will be overridden by container limits)
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     # GC settings for low-latency (G1GC is default in Java 21)
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     # Performance settings
     "-XX:+OptimizeStringConcat", \
     "-XX:+UseStringDeduplication", \
     # Security settings
     "-Djava.security.egd=file:/dev/./urandom", \
     # Run the application
     "-cp", "classes:lib/*", \
     "com.reconnoiter.api.RepoReconnoiterApplicationKt"]
