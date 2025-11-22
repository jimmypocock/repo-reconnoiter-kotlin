# RepoReconnoiter - Kotlin API

API-only backend for GitHub repository analysis using AI.

**Translation of the Rails API to Kotlin/Spring Boot for learning purposes.**

## Tech Stack

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 3.5.7
- **Java**: 21 (LTS)
- **Database**: MySQL 8.0
- **Build**: Gradle 9.2.0 (Kotlin DSL)
- **Migrations**: Flyway
- **Authentication**: JWT + API Keys (two-layer)

## Prerequisites

- Java 21+ (JDK)
- Docker & Docker Compose
- Gradle (or use included wrapper)

## Quick Start

```bash
# 1. Start MySQL database
docker-compose up -d

# 2. Set up environment variables (optional - has dev defaults)
cp .env.example .env
# Edit .env if you want to override defaults for GitHub OAuth and JWT

# 3. Run the application (dev profile active by default)
./gradlew bootRun

# API will be available at: http://localhost:8080/api/v1
```

**Note:** The `dev` profile is active by default with safe defaults for local development. See [Profiles](#profiles) for production configuration.

## OAuth Setup

### 1. Register GitHub OAuth App

Visit https://github.com/settings/developers and create a new OAuth App:

- **Application name**: RepoReconnoiter Kotlin API (Dev)
- **Homepage URL**: `http://localhost:8080`
- **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`

Copy the **Client ID** and **Client Secret** to your `.env` file.

### 2. Generate JWT Secret

```bash
# Generate a secure random string
openssl rand -base64 32
```

Add the generated secret to your `.env` file as `JWT_SECRET`.

## Development

### Running the Application

```bash
# Run in development mode (default - verbose logging)
./gradlew bootRun

# Run in production mode (minimal logging)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

# Stop: Press Ctrl+C in the terminal
```

**Note:** Spring Boot DevTools is enabled - code changes trigger automatic restart!

### Profiles

The application supports environment-specific configurations:

- **`dev`** (default): Verbose logging, SQL queries visible, safe dev defaults
- **`prod`**: Minimal logging, requires all env vars to be set explicitly
- **`console`**: No web server, database-only mode (for Gradle tasks)

**Development** (default - web mode):
```bash
./gradlew bootRun  # Uses application-dev.yml
```

**Production** (web mode):
```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun  # Uses application-prod.yml
```

**Console mode** (database tasks, no web server):
```bash
# Automatically used by Gradle tasks (apiKeyList, dbSeed, etc.)
# Profile: dev,console
# No Tomcat, no port 8080 conflict, database access only
```

### Testing

**Professional automated testing with Testcontainers - zero manual setup required.**

#### Running Tests

```bash
# Run all tests (starts MySQL container automatically)
./gradlew test

# Run specific test class
./gradlew test --tests "ComparisonRepositorySearchTest"

# Run specific test method
./gradlew test --tests "SearchSynonymExpanderTest.expand returns synonyms for known term"

# Clean build with tests
./gradlew clean build

# Build without tests (faster for production builds)
./gradlew clean build -x test
```

#### Test Infrastructure

**Testcontainers** automatically manages database containers:
- ✅ Downloads MySQL 8.0 Docker image (first time only)
- ✅ Starts container with FULLTEXT n-gram parser
- ✅ Runs Flyway migrations
- ✅ Executes all tests
- ✅ Stops and removes container
- ✅ Works identically in local dev and GitHub Actions CI/CD

**No manual setup required** - Docker handles everything!

#### Test Suites

**Unit Tests (15 tests - <1s)**
```bash
./gradlew test --tests "*SearchSynonymExpanderTest"
```
- Synonym expansion logic
- BOOLEAN MODE character sanitization
- Input validation

**Integration Tests (13 tests - ~10s)**
```bash
./gradlew test --tests "*ComparisonRepositorySearchTest"
```
- MySQL FULLTEXT search with n-gram parser
- Multi-field search (user_query, technologies, problem_domains)
- Case-insensitive matching
- Wildcard/partial matching
- SQL injection protection (8+ malicious inputs tested)
- Date filtering
- Pagination

**Application Context Test (1 test - ~5s)**
```bash
./gradlew test --tests "*RepoReconnoiterApplicationTests"
```
- Verifies Spring Boot application starts successfully
- All beans load correctly
- Database connections work

#### Test Output

Tests show detailed results in terminal:
```
ComparisonRepositorySearchTest > advancedSearch finds by user_query() PASSED
SearchSynonymExpanderTest > expand returns synonyms for known term() PASSED

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Test Results
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tests run: 29
Passed: 29
Failed: 0
Skipped: 0
Time: 114231ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Performance

- **First run**: ~1m 30s (downloads MySQL Docker image)
- **Subsequent runs**: ~10-15s (reuses cached container)
- **Unit tests only**: <1s (no database needed)

#### CI/CD Integration

GitHub Actions automatically runs tests on every push to `main`:
1. Uses existing MySQL service container (faster)
2. Testcontainers detects `DATABASE_URL` env var
3. Tests must pass before deployment proceeds

**Deployment is blocked if any test fails** - default GitHub Actions behavior stops the workflow immediately on failure.

#### Requirements

- Docker Desktop running
- Java 21
- That's it!

See the [Testing Configuration](#testing-configuration) section below for advanced details.

### Database Management

#### Starting/Stopping Database

```bash
# Start database
docker-compose up -d

# Stop database
docker-compose down
```

#### Accessing Database

**Option 1: MySQL CLI with config file (recommended)**
```bash
# One-time setup: Create mysql.cnf from example
cp mysql.cnf.example mysql.cnf
# Edit mysql.cnf if needed (default settings work for local dev)

# Connect to database (no password needed with config file)
mysql --defaults-file=mysql.cnf

# Or run quick queries
mysql --defaults-file=mysql.cnf -e "SHOW TABLES;"
mysql --defaults-file=mysql.cnf -e "SELECT * FROM users;"
```

**Option 2: Docker exec**
```bash
# Interactive MySQL shell
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev

# Quick query
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev -e "SHOW TABLES;"
```

**Option 3: GUI tool (TablePlus, DBeaver, etc.)**
- Host: `127.0.0.1`
- Port: `3306`
- Database: `reconnoiter_dev`
- Username: `reconnoiter`
- Password: `devpassword`

#### Database Tasks

```bash
# Seed database with test data
./gradlew dbSeed
```

**Migrations:**
- Flyway manages database schema automatically on startup
- Migration files: `src/main/resources/db/migration/`
- Current version displayed in logs on startup

### IDE Setup (IntelliJ IDEA)

1. Open project in IntelliJ IDEA
2. Right-click `RepoReconnoiterApplication.kt` → Run
3. Edit configuration → Add environment variables from `.env`
4. Use built-in debugger and hot reload

### Gradle Tasks (Kotlin's version of Rails rake tasks)

#### API Key Management
```bash
# Generate a new API key
./gradlew apiKeyGenerate -Pname="Insomnia Testing"
./gradlew apiKeyGenerate -Pname="Production" -Pemail="user@example.com"

# List all API keys with usage stats
./gradlew apiKeyList

# Revoke an API key
./gradlew apiKeyRevoke -Pid=123
```

#### Whitelist Management
```bash
# Add user to whitelist
./gradlew whitelistAdd -PgithubId=123 -Pusername="octocat" -Pemail="user@example.com" -Pnotes="Test user"

# List whitelisted users
./gradlew whitelistList

# Remove user from whitelist
./gradlew whitelistRemove -Pusername="octocat"
```

#### Database Tasks
```bash
# Seed database with initial data
./gradlew dbSeed
```

#### Other Useful Tasks
```bash
# Display project info
./gradlew info

# List all available tasks
./gradlew tasks --group api_keys
./gradlew tasks --group whitelist
./gradlew tasks --group database
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/reconnoiter/api/
│   │   ├── controller/     # REST controllers (@ConditionalOnWebApplication)
│   │   ├── entity/         # JPA entities (always loaded)
│   │   ├── repository/     # Data repositories (always loaded)
│   │   ├── service/        # Business logic services (always loaded)
│   │   │   ├── ApiKeyService.kt      # API key management
│   │   │   ├── WhitelistService.kt   # Whitelist management
│   │   │   ├── DatabaseSeeder.kt     # Database seeding
│   │   │   ├── AuthService.kt        # Authentication
│   │   │   └── GitHubService.kt      # GitHub API wrapper
│   │   ├── tasks/runners/  # Gradle task runners (one-off commands)
│   │   │   ├── ApiKeyListRunner.kt   # Gradle: apiKeyList
│   │   │   ├── ApiKeyGenerateRunner.kt
│   │   │   ├── WhitelistAddRunner.kt
│   │   │   └── DbSeedRunner.kt
│   │   ├── security/       # Auth filters & JWT
│   │   ├── config/         # Spring configuration
│   │   └── dto/            # Data Transfer Objects
│   └── resources/
│       ├── db/migration/            # Flyway SQL migrations
│       ├── application.yml          # Base configuration
│       ├── application-dev.yml      # Dev profile (web mode)
│       ├── application-prod.yml     # Prod profile (web mode)
│       └── application-console.yml  # Console profile (no web server)
└── test/
    └── kotlin/             # Tests (JUnit 5)
```

**Architecture Pattern:**
- **Service Layer**: Business logic (ApiKeyService, WhitelistService, etc.)
- **Task Runners**: One-off Gradle tasks (call services for admin operations)
- **Controllers**: REST API endpoints (use services for business logic)

## Security

### Input Validation & Sanitization

**Search Query Protection:**
- ✅ **BOOLEAN MODE Sanitization**: Strips special characters (`+`, `-`, `<`, `>`, `~`, `"`, `(`, `)`, `@`) to prevent search manipulation
- ✅ **Length Validation**: 255 character limit (standard HTML input max) prevents DoS attacks
- ✅ **SQL Injection Protection**: Parameterized queries with named parameters (`:searchTerms`)
- ✅ **FULLTEXT Index Security**: All indexed columns use empty strings instead of NULL (prevents NULL-based attacks)

**Implementation:**
```kotlin
// SearchSynonymExpander.kt - BOOLEAN MODE sanitization
private val BOOLEAN_MODE_SPECIAL_CHARS = Regex("[+\\-<>~\"()@]")
fun sanitizeForBooleanMode(term: String): String {
    return term.replace(BOOLEAN_MODE_SPECIAL_CHARS, "")
}

// ComparisonsController.kt - Input length validation
if (!search.isNullOrBlank() && search.length > 255) {
    throw InvalidSearchQueryException("Search query too long (max 255 characters)")
}

// ComparisonRepository.kt - Parameterized FULLTEXT queries
@Query(value = """
    SELECT DISTINCT c.* FROM comparisons c
    WHERE MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
    AGAINST(:searchTerms IN BOOLEAN MODE)
""", nativeQuery = true)
fun advancedSearch(@Param("searchTerms") searchTerms: String, ...): Page<Comparison>
```

### Security Testing

**SQL Injection Protection Tests:**
```kotlin
@Test
fun `advancedSearch protects against SQL injection attempts`() {
    val maliciousInputs = listOf(
        "'; DROP TABLE comparisons; --",
        "' OR 1=1 --",
        "' UNION SELECT * FROM users --",
        "\\' OR \\'1\\'=\\'1",
        "admin'--",
        "1' AND '1' = '1",
        "'; DELETE FROM comparisons WHERE '1'='1",
        "' OR 'x'='x'"
    )

    maliciousInputs.forEach { maliciousInput ->
        val results = comparisonRepository.advancedSearch(
            searchTerms = maliciousInput,
            startDate = null,
            endDate = null,
            pageable = PageRequest.of(0, 10)
        )

        // Should return valid Page (not execute SQL)
        assertNotNull(results)

        // Should not delete data (proves DELETE didn't execute)
        assertTrue(comparisonRepository.existsById(comparison.id!!))

        // Table should still exist (proves DROP didn't execute)
        assertTrue(comparisonRepository.count() >= 1)
    }
}
```

**All 8+ malicious SQL injection attempts are tested** and properly sanitized.

### Exception Handling

**Global Exception Handler** (`GlobalExceptionHandler.kt`):
- ✅ Sanitizes error messages (no sensitive data leaked)
- ✅ Returns consistent JSON error responses
- ✅ Includes error type, message, path, status, timestamp
- ✅ Development mode shows stack traces, production mode hides them

**Example Error Response:**
```json
{
  "error": "Bad Request",
  "message": "Search query too long (max 255 characters)",
  "path": "/api/v1/comparisons",
  "status": 400,
  "timestamp": "2025-11-22T04:13:23Z"
}
```

## Search Functionality

### MySQL FULLTEXT Search

**Advanced multi-field search with n-gram parser for technical terms.**

#### Features

- ✅ **Multi-Field Search**: Searches across `user_query`, `normalized_query`, `technologies`, `problem_domains`, `architecture_patterns`
- ✅ **N-gram Parser**: Optimized for short technical terms (api, db, job, orm, js, py, etc.)
- ✅ **BOOLEAN MODE**: Supports wildcards (`background*`), required terms (`+rails`), excluded terms (`-django`)
- ✅ **Case Insensitive**: `rails*`, `RAILS*`, `RaIlS*` all match identically
- ✅ **Synonym Expansion**: Expands search terms (e.g., "auth" → ["auth", "authentication", "authorize", "authorization"])
- ✅ **Relevance Scoring**: Results ordered by MATCH() relevance score
- ✅ **SQL Injection Protection**: Parameterized queries prevent attacks

#### N-gram FULLTEXT Index

**Migration:** `V15__add_fulltext_search_index_to_comparisons.sql`

```sql
CREATE FULLTEXT INDEX idx_comparisons_fulltext_search
ON comparisons(user_query, normalized_query, technologies, problem_domains, architecture_patterns)
WITH PARSER ngram;
```

**Benefits of N-gram Parser:**
- Handles short technical terms better than default parser
- Tokenizes text into 2-character sequences (configurable globally)
- Better for CJK languages and partial substring matching
- Ideal for API, DB, JS, etc. (terms that default parser treats as stopwords)

#### Search Implementation

**Repository Method:**
```kotlin
@Query(value = """
    SELECT DISTINCT c.*,
           MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
           AGAINST(:searchTerms IN BOOLEAN MODE) AS relevance
    FROM comparisons c
    WHERE MATCH(c.user_query, c.normalized_query, c.technologies, c.problem_domains, c.architecture_patterns)
          AGAINST(:searchTerms IN BOOLEAN MODE)
    ORDER BY relevance DESC, c.created_at DESC
""", nativeQuery = true)
fun advancedSearch(
    @Param("searchTerms") searchTerms: String,
    @Param("startDate") startDate: LocalDateTime?,
    @Param("endDate") endDate: LocalDateTime?,
    pageable: Pageable
): Page<Comparison>
```

**Synonym Expansion:**
```kotlin
// SearchSynonymExpander.kt
val SYNONYMS = mapOf(
    "auth" to setOf("auth", "authentication", "authorize", "authorization"),
    "job" to setOf("job", "jobs", "queue", "worker", "background"),
    "node" to setOf("node", "nodejs", "javascript", "js"),
    "py" to setOf("py", "python"),
    // 50+ synonym mappings...
)

fun expandQuery(query: String): Set<String> {
    val terms = query.split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { sanitizeForBooleanMode(it) }
        .filter { it.isNotEmpty() }

    return expandAll(terms)
}
```

#### Usage Example

**API Request:**
```bash
GET /api/v1/comparisons?search=background%20job&page=0&size=10
Authorization: Bearer <API_KEY>
```

**Processing:**
1. Input: `"background job"`
2. Sanitization: Remove BOOLEAN MODE special characters
3. Synonym Expansion: `"background"` → `["background"]`, `"job"` → `["job", "jobs", "queue", "worker"]`
4. FULLTEXT Query: `MATCH(...) AGAINST('background job jobs queue worker' IN BOOLEAN MODE)`
5. Results: Ordered by relevance score, paginated

#### Search Performance

**Optimized for Speed:**
- N-gram FULLTEXT index provides sub-second search
- Pagination limits memory usage
- Relevance scoring returns best matches first
- Date filters use indexed `created_at` column

**Benchmark (1000 comparisons):**
- Search with wildcards: ~50ms
- Multi-word search: ~75ms
- Synonym expansion: ~100ms

## API Endpoints

### Authentication System (Two-Layer)

This API uses **two-layer authentication** similar to the Rails version:

#### Layer 1: API Key (App-to-App)
**Required for**: ALL endpoints except `/`, `/auth/token`, `/openapi.*`

```bash
Authorization: Bearer <API_KEY>
```

Generate an API key:
```bash
./gradlew apiKeyGenerate -Pname="Insomnia Testing"
```

#### Layer 2: JWT Token (User-Specific)
**Required for**: User-specific actions (POST comparisons, POST analyses, GET profile)

```bash
X-User-Token: <JWT>
```

Exchange GitHub OAuth token for JWT:
```bash
POST /api/v1/auth/token
Content-Type: application/json

{
  "github_token": "gho_..."
}
```

### Endpoints

**Public (No Auth):**
- `GET /api/v1/` - API root and discovery

**Documentation (API Key Required):**
- `GET /api/v1/openapi.json` - OpenAPI spec (JSON)
- `GET /api/v1/openapi.yml` - OpenAPI spec (YAML)

**Authentication (API Key Required):**
- `POST /api/v1/auth/token` - Exchange GitHub token for JWT
- `GET /api/v1/profile` - Get current user profile (JWT required)

**Repositories (API Key Required):**
- `GET /api/v1/repositories` - List repositories (paginated)
- `GET /api/v1/repositories/{id}` - Get repository details
- `POST /api/v1/repositories/{id}/analyze` - Deep analysis (JWT required)
- `POST /api/v1/repositories/analyze_by_url` - Analyze by URL (JWT required)

**Comparisons (API Key Required):**
- `GET /api/v1/comparisons` - List comparisons
- `GET /api/v1/comparisons/{id}` - Get comparison details
- `POST /api/v1/comparisons` - Create comparison (JWT required)

**Admin (API Key + JWT Required):**
- `GET /api/v1/admin/stats` - System statistics

**Health Check (No Auth):**
- `GET /api/v1/actuator/health` - Health status

## AWS Production Deployment

### Architecture

**Production Infrastructure:**
- **ECS Fargate** - Serverless containers (0.5 vCPU, 1GB RAM)
- **RDS MySQL 8.0** - Managed database (db.t3.micro, 20GB storage)
- **Application Load Balancer** - HTTPS traffic with health checks
- **ECR** - Docker container registry
- **Secrets Manager** - Encrypted credentials storage
- **CloudWatch** - Logging, monitoring, and alarms

### Deployment Workflow

**Fresh Deployment (After Infrastructure Teardown):**

```bash
# 1. Deploy infrastructure with 0 tasks (waits for Docker image)
cd cdk
npm run deploy:api:fresh  # Uses --context desiredCount=0

# 2. Build and push Docker image (from project root)
cd ..
docker build --platform linux/amd64 -t repo-reconnoiter:latest .

# Get ECR URI from CloudFormation outputs
docker tag repo-reconnoiter:latest <ECR_URI>:latest
docker tag repo-reconnoiter:latest <ECR_URI>:v1.0.0

# Login and push
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_URI>
docker push <ECR_URI>:latest
docker push <ECR_URI>:v1.0.0

# 3. Redeploy with tasks (production-ready default: 2 tasks)
cd cdk
npm run deploy:api  # Defaults to desiredCount=2
```

**Normal Deployments (Code Changes):**

```bash
# Code is production-ready by default - just deploy
cd cdk
npm run deploy:api
```

The CDK code defaults to `desiredCount: 2` (production-ready), but can be overridden with `--context desiredCount=0` for fresh deployments.

### Database Seeding (Production)

**Using Flyway Migrations (Recommended):**

```bash
# 1. Create seed migration
# File: src/main/resources/db/migration/V17__seed_categories.sql
INSERT INTO categories (name, category_type, description, created_at, updated_at) VALUES
('Ruby', 'LANGUAGE', 'Ruby programming language', NOW(), NOW()),
('Python', 'LANGUAGE', 'Python programming language', NOW(), NOW());

# 2. Redeploy API - Flyway runs migrations on container startup automatically
cd cdk
npm run deploy:api
```

**Using Gradle Tasks (For Seeding from JSON Files):**

The production Docker image includes Gradle and JDK for running admin tasks via ECS Exec.

```bash
# 1. Get running task ID
aws ecs list-tasks --cluster repo-reconnoiter-prod --service-name repo-reconnoiter-prod

# 2. Connect to container
aws ecs execute-command \
  --cluster repo-reconnoiter-prod \
  --task <TASK_ID> \
  --container repo-reconnoiter-api \
  --interactive \
  --command "/bin/bash"

# 3. Inside container - Run Gradle tasks with memory limits
# IMPORTANT: Use --no-daemon and limit heap to prevent OOM kills

# Seed database from JSON files
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  dbSeed

# Generate API key
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  apiKeyGenerate -Pname="Production Key"

# Add user to whitelist
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  whitelistAdd -PgithubId=<ID> -Pusername="<USERNAME>" -Pemail="<EMAIL>"

# Exit container
exit
```

**Why memory limits are required:**
- Container has 2GB RAM (configured in `cdk/lib/api-stack.ts`)
- Spring Boot app uses ~400MB
- Without limits, Gradle tries to allocate 2GB heap and gets OOM killed
- With `-Xmx768m`, Gradle + Spring Boot fit within 2GB limit (~1.2GB total)

**Direct Database Access:**

```bash
# Option 1: ECS Exec into running container
aws ecs execute-command \
  --cluster repo-reconnoiter-prod \
  --task <TASK_ID> \
  --container repo-reconnoiter-api \
  --interactive \
  --command "/bin/bash"

# Then: mysql -h <RDS_HOST> -u reconnoiter -p

# Option 2: Connect directly from local machine
# (Not possible - RDS is in isolated subnet with no internet access)
```

### Key Configuration Notes

**Health Check Paths:**
- ALB health check: `/api/v1/actuator/health`
- Container health check: `http://localhost:8080/api/v1/actuator/health`
- Spring Boot context path: `/api/v1` (must be included in health check paths)

**Platform Requirements:**
- ECS Fargate requires `linux/amd64` Docker images
- M-series Macs must use `--platform linux/amd64` flag (slower but required)
- Production CI/CD (GitHub Actions) runs on AMD64 natively

**Stack Architecture:**
- Modular CDK stacks: Network → SecurityGroups → Database → API
- Clean deletion supported (no RETAIN policies except production RDS snapshots)
- SNS email notifications for CloudWatch alarms (requires confirmation)

See **[cdk/README.md](cdk/README.md)** and **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** for detailed deployment instructions.

## Testing Configuration

### Testcontainers Architecture

**Automatic database container management for integration tests.**

#### How It Works

**Local Development:**
1. Run `./gradlew test`
2. Testcontainers detects Docker is available
3. Downloads MySQL 8.0 image (first time only)
4. Starts MySQL container with test configuration
5. Runs Flyway migrations automatically
6. Executes all tests
7. Stops and removes container

**GitHub Actions CI/CD:**
1. Workflow starts MySQL service container (`.github/workflows/deploy.yml`)
2. Sets `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` env vars
3. Testcontainers detects env vars and uses existing container
4. Tests run faster (no container startup overhead)
5. Workflow continues if tests pass, stops if tests fail

#### Configuration Files

**TestcontainersConfiguration.kt:**
```kotlin
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
                // Enable FULLTEXT n-gram parser (matches production)
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
```

**application-test.yml:**
```yaml
spring:
  # Datasource properties automatically configured by Testcontainers @ServiceConnection
  # When running in CI/CD, DATABASE_URL env var overrides Testcontainers
  datasource:
    url: ${DATABASE_URL:}  # Empty default - Testcontainers provides value
    username: ${DATABASE_USERNAME:}
    password: ${DATABASE_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: none  # Don't auto-create schema - use Flyway migrations
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    baseline-version: 0
```

**Test Class Example:**
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration::class)  // Import Testcontainers config
@ActiveProfiles("test")
class ComparisonRepositorySearchTest {
    // Tests automatically use Testcontainers MySQL
}
```

#### Key Features

**Spring Boot 3.1+ @ServiceConnection:**
- Automatically configures datasource properties from container
- No manual JDBC URL configuration needed
- Works with MySQL, PostgreSQL, Redis, Kafka, etc.

**Container Reuse:**
- `withReuse(true)` keeps container running between test classes
- Speeds up test execution (~10s vs ~30s for fresh container)
- Container stopped when Gradle daemon stops or Docker restarts

**Transaction Management:**
- Tests use `TransactionTemplate` for explicit commits
- MySQL InnoDB FULLTEXT indexes require committed data
- `@DirtiesContext` ensures clean state between tests

#### Troubleshooting

**"Could not find or load main class org.testcontainers.utility.TestcontainersConfiguration"**

Gradle cache issue. Fix with:
```bash
./gradlew clean build --refresh-dependencies
```

**"Cannot connect to Docker daemon"**

Ensure Docker Desktop is running:
```bash
docker ps  # Should list running containers
```

**Tests hang during container startup**

Check Docker Desktop resource limits:
- **Minimum**: 2 CPU cores, 4GB RAM
- **Recommended**: 4 CPU cores, 8GB RAM

**"Container startup failed: port already in use"**

Another MySQL instance is running on port 3306:
```bash
# Stop local MySQL
docker-compose down

# Or kill the process using port 3306
lsof -ti:3306 | xargs kill -9
```

#### Dependencies

**build.gradle.kts:**
```kotlin
dependencies {
    // Testcontainers for automated database testing
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

**Version managed by Spring Boot BOM** - no explicit versions needed!

#### GitHub Actions Workflow

**.github/workflows/deploy.yml:**
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest

    # MySQL service for tests
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_DATABASE: reconnoiter_test
          MYSQL_USER: reconnoiter
          MYSQL_PASSWORD: testpassword
          MYSQL_ROOT_PASSWORD: rootpassword
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h localhost"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    steps:
      # ... checkout, setup JDK ...

      - name: Run tests
        env:
          DATABASE_URL: jdbc:mysql://localhost:3306/reconnoiter_test
          DATABASE_USERNAME: reconnoiter
          DATABASE_PASSWORD: testpassword
        run: ./gradlew test --no-daemon

      # ... build and deploy ONLY if tests pass ...
```

**Test flow in CI:**
1. MySQL service starts before tests
2. Tests detect `DATABASE_URL` env var
3. Testcontainers uses existing service container
4. Tests complete in ~2 minutes
5. Workflow stops if any test fails
6. Build/deploy steps only run if tests pass

## Reference Implementation

This is a port of the Rails API found in the parent directory.

**Rails Repo**: `../` (root of monorepo)
