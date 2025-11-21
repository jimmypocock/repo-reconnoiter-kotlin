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

## Reference Implementation

This is a port of the Rails API found in the parent directory.

**Rails Repo**: `../` (root of monorepo)
