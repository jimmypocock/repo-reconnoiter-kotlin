# RepoReconnoiter - Kotlin API

API-only backend for GitHub repository analysis using AI.

**Translation of the Rails API to Kotlin/Spring Boot for learning purposes.**

## Tech Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.7
- **Java**: 21 (LTS)
- **Database**: MySQL 8.0
- **Build**: Gradle (Kotlin DSL)

## Prerequisites

- Java 21+ (JDK)
- Docker & Docker Compose
- Gradle (or use included wrapper)

## Quick Start

```bash
# 1. Start MySQL database
docker-compose up -d

# 2. Set up environment variables
cp .env.example .env
# Edit .env and add your GitHub OAuth credentials and JWT secret

# 3. Build the project
./gradlew build

# 4. Run the application
./gradlew bootRun

# API will be available at: http://localhost:8080/api/v1
```

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
# Run with auto-reload (recommended for development)
./gradlew bootRun --continuous

# Or standard run
./gradlew bootRun

# Stop: Press Ctrl+C in the terminal
```

**Note:** Spring Boot DevTools is enabled - code changes trigger automatic restart!

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

# View database
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev

# Check database tables
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev -e "SHOW TABLES;"
```

### IDE Setup (IntelliJ IDEA)

1. Open project in IntelliJ IDEA
2. Right-click `KotlinApiApplication.kt` → Run
3. Edit configuration → Add environment variables from `.env`
4. Use built-in debugger and hot reload

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/reconnoiter/api/
│   │   ├── controller/     # REST controllers
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Business logic
│   │   └── config/         # Configuration
│   └── resources/
│       └── application.yml # Configuration
└── test/
    └── kotlin/             # Tests (JUnit 5)
```

## API Endpoints

**Health Check:**
- `GET /api/v1/actuator/health`

**Authentication:**
- `GET /oauth2/authorization/github` - Initiate GitHub OAuth login
- `GET /api/v1/profile` - Get current user profile (requires JWT)

**Repositories (Public):**
- `GET /api/v1/repositories` - List repositories (paginated)
- `GET /api/v1/repositories/{id}` - Get repository details

**Analysis (Protected - requires X-User-Token header):**
- `POST /api/v1/repositories/{id}/analyze` - Deep analysis (coming soon)
- `POST /api/v1/repositories/analyze_by_url` - Analyze by GitHub URL (coming soon)

**Comparisons (Public read, Protected write):**
- `GET /api/v1/comparisons` - List comparisons
- `POST /api/v1/comparisons` - Create comparison (coming soon)

## Reference Implementation

This is a port of the Rails API found in the parent directory.

**Rails Repo**: `../` (root of monorepo)
