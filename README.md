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

**Development** (default):
```bash
./gradlew bootRun  # Uses application-dev.yml
```

**Production**:
```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun  # Uses application-prod.yml
```

### Testing & Building

```bash
# Run all tests
./gradlew test

# Clean build (includes tests)
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test
```

### Database Management

```bash
# Start database
docker-compose up -d

# Stop database
docker-compose down

# View database (interactive)
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev

# Check database tables
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev -e "SHOW TABLES;"

# Seed database with test data
./gradlew dbSeed
```

**Migrations:**
- Flyway manages database schema automatically on startup
- Migration files: `src/main/resources/db/migration/`
- Current version displayed in logs on startup

### IDE Setup (IntelliJ IDEA)

1. Open project in IntelliJ IDEA
2. Right-click `KotlinApiApplication.kt` → Run
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
│   │   ├── controller/     # REST controllers
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Business logic
│   │   ├── security/       # Auth filters & JWT
│   │   ├── config/         # Spring configuration
│   │   └── tasks/          # Gradle task scripts
│   └── resources/
│       ├── db/migration/   # Flyway SQL migrations
│       ├── application.yml # Base configuration
│       ├── application-dev.yml   # Dev profile config
│       └── application-prod.yml  # Production config
└── test/
    └── kotlin/             # Tests (JUnit 5)
```

## API Endpoints

### Authentication System (Two-Layer)

This API uses **two-layer authentication** similar to the Rails version:

#### Layer 1: API Key (App-to-App)
**Required for**: ALL endpoints except `/`, `/auth/exchange`, `/openapi.*`

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
POST /api/v1/auth/exchange
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
- `POST /api/v1/auth/exchange` - Exchange GitHub token for JWT
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

## Reference Implementation

This is a port of the Rails API found in the parent directory.

**Rails Repo**: `../` (root of monorepo)
