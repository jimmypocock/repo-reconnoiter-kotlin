# AWS Deployment Game Plan

## Deployment Status

✅ **SUCCESSFULLY DEPLOYED TO PRODUCTION**
- **Date:** November 21, 2025
- **Environment:** Production (repo-reconnoiter-prod)
- **API URL:** `http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com`
- **Status:** 2 healthy ECS tasks running, ALB health checks passing

## Lessons Learned

### Critical Health Check Configuration

**Problem:** ECS tasks were RUNNING but UNHEALTHY, causing CloudFormation to hang during deployment.

**Root Cause:** Spring Boot uses context path `/api/v1`, but health checks were configured for `/actuator/health` (wrong path).

**Solution:** Updated both health checks to include context path:
- ALB Target Group: `/api/v1/actuator/health`
- Container Health Check: `curl -f http://localhost:8080/api/v1/actuator/health`

**Key Takeaway:** Infrastructure health checks must match application routing configuration. Spring Boot's `server.servlet.context-path` affects all endpoints including actuator.

### Professional Two-Phase Deployment Pattern

**Challenge:** CloudFormation can't deploy ECS service before Docker image exists in ECR.

**Solution:** Use CDK context variable for `desiredCount`:
```typescript
const desiredCount = this.node.tryGetContext('desiredCount') ?? 2;
```

**Workflow:**
1. **Fresh deployment:** `npm run deploy:api:fresh` (desiredCount: 0)
2. **Build and push:** Docker image to ECR
3. **Scale up:** `npm run deploy:api` (desiredCount: 2, production default)

**Benefits:**
- Code is production-ready by default (no code changes needed)
- Override available for fresh deployments (`--context desiredCount=0`)
- No stuck CloudFormation deployments waiting for non-existent images

### Docker Platform Requirements

**Problem:** ECS tasks fail with `CannotPullContainerError` on Apple Silicon Macs.

**Root Cause:** Docker images built on M-series Macs default to ARM64, but ECS Fargate requires AMD64.

**Solution:** Always use `--platform linux/amd64` flag:
```bash
docker build --platform linux/amd64 -t repo-reconnoiter:latest .
```

**Future:** Set up GitHub Actions CI/CD (runs on AMD64 natively, no platform issues).

### CloudWatch Alarm Configuration

**Problem:** Emojis in alarm descriptions cause CloudFormation deployment failure.

**Error:** `AlarmDescription must not contain restricted XML characters`

**Solution:** Use text markers instead: `[CRITICAL]` and `[WARNING]`

**Takeaway:** CloudWatch stores alarm descriptions as XML - avoid emojis and special characters.

### SNS Topic Drift

**Problem:** CloudWatch alarms show "We could not find the SNS topic" warning despite CloudFormation showing `CREATE_COMPLETE`.

**Root Cause:** SNS topic was manually deleted but CloudFormation state wasn't updated (drift).

**Solution:** Manually recreate topic matching CloudFormation's expected ARN, then resubscribe email.

**Prevention:** Use `cdk destroy` instead of manually deleting resources.

### Stack Naming and Organization

**Architecture:** Modular CDK stacks for clean separation of concerns:
- `RepoReconnoiter-Network-prod` - VPC, subnets, NAT Gateway
- `RepoReconnoiter-SecurityGroups-prod` - ALB, ECS, and RDS security groups
- `RepoReconnoiter-Database-prod` - RDS MySQL with CloudWatch alarms
- `RepoReconnoiter-API-prod` - ECS Fargate, ALB, ECR, CloudWatch logs

**Benefits:**
- Independent stack updates (can update API without touching database)
- Clean deletion (drop stacks in reverse dependency order)
- Clear resource ownership and naming conventions

### Database Seeding Strategy

**Recommended:** Flyway versioned migrations (`V17__seed_categories.sql`)
- Runs automatically on container startup
- Version controlled and repeatable
- Integrates with existing migration workflow

**Alternative:** Direct database access via ECS Exec or MySQL client
- Useful for testing and ad-hoc operations
- Requires temporary security group rules for external access

**Key Insight:** Kotlin/Spring Boot follows infrastructure-as-code principles - use Flyway migrations for seeding rather than custom Gradle tasks.

---

## Project Context (For Fresh Claude Instance)

### What We're Deploying

**Application:** RepoReconnoiter Kotlin API
**Purpose:** GitHub repository analysis and comparison service using OpenAI
**Architecture:** Spring Boot 3.5.7 REST API with MySQL database
**Current State:** Fully developed, tested locally, ready for production deployment

### Tech Stack

**Backend:**
- Spring Boot 3.5.7 (Kotlin 2.2.21, Java 21)
- Spring WebFlux (WebClient for GitHub API)
- Spring Security (two-layer authentication)
- Spring Data JPA + Hibernate
- MySQL 8.0 (with Flyway migrations)
- Sentry (error tracking)

**Infrastructure:**
- Docker (multi-stage production build ready)
- AWS ECS Fargate (chosen over App Runner for production-grade control)
- AWS RDS MySQL 8.0
- AWS Application Load Balancer
- AWS Secrets Manager
- CloudWatch (monitoring)

**Authentication:**
- **Layer 1:** API Key (`Authorization: Bearer <API_KEY>`) - Required for ALL endpoints
- **Layer 2:** JWT (`X-User-Token: <JWT>`) - Required for user-specific endpoints
- This prevents users from bypassing the frontend and calling expensive OpenAI operations directly

### Key Architectural Decisions

1. **Two-Layer Auth:** Protects expensive OpenAI operations from direct API access
2. **ECS Fargate over App Runner:** Better VPC integration, production-grade monitoring, future-proof
3. **WebClient over RestTemplate:** Modern, non-blocking HTTP client with timeouts
4. **Selective Error Catching:** User errors (401/403) handled gracefully, system errors bubble to Sentry
5. **N-gram FULLTEXT Search:** MySQL full-text search for short technical terms (api, db, job, etc.)

### Current Development Setup

**Local Development:**
```bash
docker-compose up -d          # MySQL + Application
./gradlew bootRun             # Run app directly (faster iteration)
./gradlew compileKotlin       # Compile check
```

**Database:**
- MySQL 8.0 running in Docker
- 16 Flyway migrations (all production-ready)
- Schema includes: users, repositories, comparisons, analyses, api_keys, etc.

**Docker:**
- Multi-stage build (eclipse-temurin:21-jdk → eclipse-temurin:21-jre)
- Non-root user (spring:spring)
- Health check configured
- Port 8080 (Spring Boot default)

### Required Environment Variables

**Production Environment Variables:**
```bash
# Database
DATABASE_HOST=<rds-endpoint>
DATABASE_PORT=3306
DATABASE_NAME=reconnoiter_prod
DATABASE_USERNAME=reconnoiter
DATABASE_PASSWORD=<from-secrets-manager>

# Application
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=<from-secrets-manager>
SERVER_PORT=8080

# GitHub OAuth
GITHUB_CLIENT_ID=<from-secrets-manager>
GITHUB_CLIENT_SECRET=<from-secrets-manager>

# Sentry (Error Tracking)
SENTRY_DSN=<your-sentry-dsn>

# Optional
ALLOWED_ORIGINS=https://yourdomain.com
```

### Endpoints Overview

**Public (No Auth):**
- `GET /` - Homepage
- `GET /actuator/health` - Health check
- `GET /openapi.json` - API docs

**API Key Required:**
- `POST /auth/token` - Exchange GitHub token for JWT
- `GET /comparisons` - List comparisons
- `GET /comparisons/:id` - Show comparison
- `GET /repositories` - List repositories
- `GET /repositories/:id` - Show repository

**API Key + JWT Required:**
- `GET /profile` - User profile
- `POST /comparisons` - Create comparison (uses OpenAI, costs money) (Not yet implemented)
- `POST /repositories/analyze_by_url` - Create analysis (uses OpenAI, costs money) (Not yet implemented)
- `POST /repositories/:id/analyze` - Create analysis (uses OpenAI, costs money) (Not yet implemented)

### Pre-Deployment Checklist

**Already Complete ✅:**
- [x] Application code production-ready
- [x] Docker multi-stage build optimized
- [x] Health checks configured
- [x] Secrets use environment variables
- [x] Database migrations ready (16 migrations)
- [x] Two-layer authentication working
- [x] WebClient timeouts configured
- [x] Sentry error tracking configured
- [x] CORS configured
- [x] N-gram FULLTEXT search implemented

**To Complete During Deployment:**

### Phase 0: AWS Account Setup (One-Time)
- [ ] AWS account created
- [ ] AWS CLI installed: `aws --version`
- [ ] AWS credentials configured: `aws configure`
- [ ] Verify credentials: `aws sts get-caller-identity`
- [ ] CDK CLI installed: `npm install -g aws-cdk`
- [ ] Bootstrap CDK: `cdk bootstrap aws://ACCOUNT-ID/us-east-1`

### Phase 1: Verify CDK Environment Variables (~5 minutes)

**⚠️ CRITICAL: Verify all required environment variables are configured in CDK task definition**

Check `cdk/lib/repo-reconnoiter-stack.ts` includes ALL required variables:

**Required Environment Variables:**
- [ ] `SPRING_PROFILES_ACTIVE` = prod
- [ ] `SERVER_PORT` = 8080
- [ ] `DATABASE_URL` = jdbc:mysql://[RDS_ENDPOINT]:3306/reconnoiter
- [ ] `DATABASE_USERNAME` = reconnoiter
- [ ] `APP_FRONTEND_URL` = https://reporeconnoiter.com (or your frontend URL)
- [ ] `SENTRY_DSN` = https://placeholder@sentry.io/placeholder (update later)

**Required Secrets (from Secrets Manager):**
- [ ] `DATABASE_PASSWORD` (from dbSecret)
- [ ] `JWT_SECRET` (from jwtSecret)
- [ ] `GITHUB_CLIENT_ID` (from githubSecret)
- [ ] `GITHUB_CLIENT_SECRET` (from githubSecret)
- [ ] `OPENAI_ACCESS_TOKEN` (from openaiSecret)

**Common Pitfalls:**
- ❌ Missing `SENTRY_DSN` → App crashes on startup with "Could not resolve placeholder"
- ❌ Missing `APP_FRONTEND_URL` → OAuth redirects fail, CORS errors

### Phase 2: Build and Push Docker Image FIRST (~10 minutes)

**⚠️ IMPORTANT: Push Docker image BEFORE deploying ECS service!**

ECS service won't stabilize until a valid image exists in ECR. Deploying infrastructure first will cause CloudFormation to wait/timeout.

**For Apple Silicon Macs (M1/M2/M3):**
- ECS Fargate requires `linux/amd64` images (x86_64 architecture)
- M-series Macs default to `linux/arm64` builds
- Must use `--platform linux/amd64` flag (uses QEMU emulation, slower build)

**Commands:**
- [ ] Go to project root: `cd /path/to/repo-reconnoiter-kotlin`
- [ ] Load AWS credentials: `source cdk/.env && export $(grep -v '^#' cdk/.env | xargs)`
- [ ] Login to ECR: `aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 626635410245.dkr.ecr.us-east-1.amazonaws.com`
- [ ] Build and push for AMD64: `docker buildx build --platform linux/amd64 --push -t 626635410245.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter-prod:latest .`
- [ ] Verify image: `aws ecr describe-images --repository-name repo-reconnoiter-prod`

**Note:** Docker containers are NOT virtual machines - they share the host kernel and must match the target architecture. In production, use CI/CD (GitHub Actions on AMD64 runners) to avoid platform mismatch issues.

### Phase 3: Deploy CDK Infrastructure (~10-15 minutes)
- [ ] Install CDK dependencies: `cd cdk && npm install`
- [ ] Build CDK: `npm run build`
- [ ] Preview changes: `npm run diff`
- [ ] Deploy stack: `npm run deploy`
- [ ] Save outputs (ECR URI, RDS endpoint, Secret ARNs, ALB URL)

### Phase 4: Configure Secrets (~5 minutes)
- [ ] Update GitHub OAuth secret (see commands in CDK README)
- [ ] Update OpenAI API key secret (see commands in CDK README)
- [ ] Verify secrets: `aws secretsmanager list-secrets | grep repo-reconnoiter`

### Phase 5: Run Database Migrations (~5 minutes)
- [ ] Get DB password from Secrets Manager
- [ ] Run Flyway migrations locally (or let ECS run on startup)
- [ ] Verify migrations: Connect to RDS and check `flyway_schema_history`

### Phase 6: Verify ECS Service (Automatic via CDK)
- [ ] ECS service automatically created by CDK
- [ ] Tasks start and pull Docker image from ECR
- [ ] Health checks pass at `/actuator/health`
- [ ] ALB routes traffic to healthy tasks

### Phase 7: Verify Deployment (~10 minutes)
- [ ] Check ECS service status: `aws ecs describe-services...`
- [ ] Check ECS task health: `aws ecs describe-tasks...`
- [ ] View logs: `aws logs tail /ecs/repo-reconnoiter-dev --follow`
- [ ] Test health endpoint: `curl https://ALB_URL/actuator/health`
- [ ] Test API endpoint: `curl -H "Authorization: Bearer API_KEY" https://ALB_URL/api/v1/repositories`

### Phase 8: Production Readiness (~5 minutes)
- [ ] Add ACM SSL certificate to ALB (manual in AWS Console)
- [ ] Configure HTTP → HTTPS redirect in ALB listener (port 80 → 443)
  - Update CDK: Change `httpListener.addTargets()` to `httpListener.addAction()` with redirect
  - Keep port 80 open on security group (for redirect, not direct access)
- [ ] Generate production API keys
- [ ] Verify Sentry error tracking works
- [ ] Set up CloudWatch alarms (optional but recommended)
- [ ] Update DNS to point to ALB (optional - use custom domain)
- [ ] 🎉 Go live!

### Files to Reference

**Application Code:**
- `src/main/kotlin/com/reconnoiter/api/` - Main application code
- `src/main/resources/application.yml` - Configuration
- `src/main/resources/db/migration/` - Database migrations
- `Dockerfile` - Production-ready Docker build
- `docker-compose.yml` - Local development setup

**Configuration:**
- `.env.example` - All environment variables documented
- `build.gradle.kts` - Dependencies and build configuration

### Success Criteria

**Deployment is successful when:**
1. ✅ `curl https://api.yourdomain.com/actuator/health` returns `{"status":"UP"}`
2. ✅ All 16 Flyway migrations applied successfully
3. ✅ Can exchange GitHub token for JWT: `POST /auth/token`
4. ✅ Can access user profile with JWT: `GET /profile`
5. ✅ CloudWatch logs showing application startup
6. ✅ Sentry receiving error reports (test with `/test/sentry`)
7. ✅ No 5xx errors in first hour of production traffic

### Timeline Goal

**Target:** Live by dinner tomorrow (ideally 6pm)
**Estimated:** ~5 hours of focused work
**Buffer:** 3 hours for troubleshooting
**Total:** 8 hours → Start 10am, done by 6pm ✅

---

## Pre-Deployment Status ✅

**Good News - Most production requirements are already in place:**
- ✅ Secrets use environment variables (no hardcoded values)
- ✅ Docker multi-stage build optimized
- ✅ Non-root user (spring:spring)
- ✅ Health check configured
- ✅ WebClient timeouts set
- ✅ Two-layer authentication (API key + JWT)
- ✅ Sentry configured (error tracking ready)
- ✅ CORS configured
- ✅ HTTPS enforcement ready

**Items to Review Tomorrow Morning:**
1. Production Spring profile configuration
2. Database connection pool tuning
3. Rate limiting verification
4. CloudWatch logging setup

---

## Tomorrow's Deployment Plan

### Phase 1: AWS Infrastructure Setup (CDK) - 2-3 hours

**1.1 Create CDK Project Structure**
```bash
mkdir cdk
cd cdk
npx aws-cdk init app --language=typescript
```

**1.2 Core Infrastructure to Define:**

**VPC & Networking:**
- VPC with public/private subnets
- NAT Gateway for private subnets
- Security groups for RDS and ECS

**RDS MySQL Database:**
```typescript
const database = new rds.DatabaseInstance(this, 'Database', {
  engine: rds.DatabaseInstanceEngine.mysql({
    version: rds.MysqlEngineVersion.VER_8_0
  }),
  instanceType: ec2.InstanceType.of(
    ec2.InstanceClass.T3,
    ec2.InstanceSize.MICRO  // Start small, scale up
  ),
  vpc: vpc,
  vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
  multiAz: false,  // Enable for production redundancy
  allocatedStorage: 20,
  storageEncrypted: true,
  backupRetention: Duration.days(7),
  deletionProtection: true
});
```

**ECS Fargate Cluster:**
```typescript
const cluster = new ecs.Cluster(this, 'Cluster', {
  vpc: vpc,
  containerInsights: true  // CloudWatch monitoring
});

const taskDefinition = new ecs.FargateTaskDefinition(this, 'Task', {
  memoryLimitMiB: 512,
  cpu: 256
});

const container = taskDefinition.addContainer('app', {
  image: ecs.ContainerImage.fromRegistry('your-ecr-repo'),
  logging: ecs.LogDrivers.awsLogs({
    streamPrefix: 'repo-reconnoiter',
    logRetention: logs.RetentionDays.ONE_WEEK
  }),
  environment: {
    SPRING_PROFILES_ACTIVE: 'prod',
    DATABASE_HOST: database.dbInstanceEndpointAddress
  },
  secrets: {
    DATABASE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret),
    JWT_SECRET: ecs.Secret.fromSecretsManager(jwtSecret),
    GITHUB_CLIENT_SECRET: ecs.Secret.fromSecretsManager(githubSecret)
  }
});
```

**Application Load Balancer:**
```typescript
const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
  vpc: vpc,
  internetFacing: true
});

const listener = alb.addListener('Listener', {
  port: 443,
  certificates: [certificate],  // ACM certificate
  defaultAction: elbv2.ListenerAction.forward([targetGroup])
});
```

**1.3 Secrets Manager:**
```typescript
const dbSecret = new secretsmanager.Secret(this, 'DBPassword', {
  generateSecretString: {
    secretStringTemplate: JSON.stringify({ username: 'admin' }),
    generateStringKey: 'password',
    excludePunctuation: true
  }
});

const jwtSecret = new secretsmanager.Secret(this, 'JWTSecret', {
  generateSecretString: { excludePunctuation: true }
});
```

**1.4 Route 53 & ACM:**
```typescript
const hostedZone = route53.HostedZone.fromLookup(this, 'Zone', {
  domainName: 'yourdomain.com'
});

const certificate = new acm.Certificate(this, 'Certificate', {
  domainName: 'api.yourdomain.com',
  validation: acm.CertificateValidation.fromDns(hostedZone)
});
```

---

### Phase 2: Database Migration - 30 minutes

**2.1 Create Database & User:**
```bash
# Connect to RDS instance
mysql -h <rds-endpoint> -u admin -p

CREATE DATABASE reconnoiter_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'reconnoiter'@'%' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON reconnoiter_prod.* TO 'reconnoiter'@'%';
FLUSH PRIVILEGES;
```

**2.2 Run Flyway Migrations:**
```bash
# Option 1: Run from ECS task
aws ecs run-task --cluster <cluster> --task-definition <task-def> \
  --overrides '{"containerOverrides":[{"name":"app","command":["./gradlew","flywayMigrate"]}]}'

# Option 2: Run locally with production credentials
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:mysql://<rds-endpoint>:3306/reconnoiter_prod \
DATABASE_USERNAME=reconnoiter \
DATABASE_PASSWORD=<password> \
./gradlew flywayMigrate
```

---

### Phase 3: Container Registry - 20 minutes

**3.1 Create ECR Repository:**
```bash
aws ecr create-repository --repository-name repo-reconnoiter
```

**3.2 Build & Push Image:**
```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build for production
docker build -t repo-reconnoiter:latest .

# Tag for ECR
docker tag repo-reconnoiter:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter:latest
```

---

### Phase 4: Deploy Application - 30 minutes

**4.1 Deploy CDK Stack:**
```bash
cd cdk
npm install
cdk bootstrap  # One-time setup
cdk synth      # Verify CloudFormation template
cdk deploy --require-approval never
```

**4.2 Verify Deployment:**
```bash
# Check ECS service status
aws ecs describe-services --cluster <cluster> --services <service>

# Check task health
aws ecs describe-tasks --cluster <cluster> --tasks <task-arn>

# View logs
aws logs tail /ecs/repo-reconnoiter --follow
```

**4.3 Test Endpoints:**
```bash
# Health check
curl https://api.yourdomain.com/actuator/health

# API test
curl -H "Authorization: Bearer <api-key>" \
  https://api.yourdomain.com/comparisons
```

---

### Phase 5: Monitoring & Alerts - 30 minutes

**5.1 CloudWatch Dashboards:**
- ECS task CPU/memory usage
- ALB request count/latency
- RDS connections/query performance
- Application error rate

**5.2 CloudWatch Alarms:**
```typescript
const errorAlarm = new cloudwatch.Alarm(this, 'ErrorAlarm', {
  metric: targetGroup.metrics.httpCodeTarget(
    elbv2.HttpCodeTarget.TARGET_5XX_COUNT
  ),
  threshold: 10,
  evaluationPeriods: 2,
  alarmDescription: '5xx errors detected'
});

errorAlarm.addAlarmAction(new cwactions.SnsAction(alertTopic));
```

**5.3 Sentry Integration:**
- Verify Sentry DSN in production environment variables
- Test error reporting: `curl https://api.yourdomain.com/test/sentry`

---

### Phase 6: Production Checklist - 15 minutes

**Before Going Live:**
- [ ] Database backups enabled (RDS automatic backups)
- [ ] SSL certificate valid and auto-renewing
- [ ] Secrets rotated from defaults
- [ ] Rate limiting tested
- [ ] Load testing completed
- [ ] Rollback plan documented
- [ ] DNS propagation verified
- [ ] CORS origins set to production frontend
- [ ] API keys generated for production
- [ ] Monitoring dashboards configured
- [ ] On-call alerts set up

---

## Estimated Timeline

| Phase | Duration | Cumulative |
|-------|----------|------------|
| CDK Infrastructure | 2-3 hours | 3 hours |
| Database Migration | 30 min | 3.5 hours |
| Container Registry | 20 min | 3.75 hours |
| Deploy Application | 30 min | 4.25 hours |
| Monitoring Setup | 30 min | 4.75 hours |
| Production Checklist | 15 min | **5 hours** |

**Total: ~5 hours** (with buffer for troubleshooting)

---

## Cost Estimates (AWS)

**Monthly costs:**
- RDS MySQL (t3.micro): ~$15/month
- ECS Fargate (0.25 vCPU, 0.5 GB): ~$15/month
- ALB: ~$20/month
- NAT Gateway: ~$32/month
- Data transfer: ~$5/month
- **Total: ~$87/month**

**Cost Optimization:**
- Use RDS reserved instances (save 40%)
- Use VPC endpoints to avoid NAT Gateway ($32 savings)
- Scale down ECS task count during off-hours

---

## Tomorrow's Workflow

**Morning (9am-12pm):**
1. ☕ Review this guide
2. Create CDK project
3. Define infrastructure (RDS, ECS, ALB)
4. Deploy CDK stack
5. Run database migrations

**Afternoon (1pm-4pm):**
6. Build and push Docker image
7. Deploy application
8. Configure monitoring
9. Test all endpoints
10. Go live!

**Evening (5pm):**
11. 🎉 Celebrate deployment
12. Monitor for first few hours

---

## Troubleshooting Guide

**Common Issues:**

**1. Task fails to start:**
```bash
# Check task logs
aws logs tail /ecs/repo-reconnoiter --since 10m

# Check task stopped reason
aws ecs describe-tasks --cluster <cluster> --tasks <task-arn> \
  --query 'tasks[0].stoppedReason'
```

**2. Database connection fails:**
- Verify security group allows ECS → RDS (port 3306)
- Check DATABASE_URL format
- Verify secrets are properly injected

**3. Health check fails:**
- Check /actuator/health returns 200
- Verify ALB target group health check path
- Check security group allows ALB → ECS (port 8080)

**4. 502 Bad Gateway:**
- ECS task not healthy
- Application not listening on port 8080
- Check application logs

---

## Rollback Plan

**If deployment fails:**
```bash
# Rollback CDK stack
cdk deploy --rollback

# Rollback to previous ECS task revision
aws ecs update-service --cluster <cluster> --service <service> \
  --task-definition <previous-revision>

# Database rollback (if needed)
# Restore from RDS automated backup
```

---

## Post-Deployment

**Week 1:**
- Monitor error rates daily
- Review CloudWatch dashboards
- Optimize database queries
- Fine-tune auto-scaling

**Week 2:**
- Review costs
- Set up automated backups beyond RDS
- Document runbooks
- **Set up CI/CD pipeline (GitHub Actions)** - See section below ⬇️

**Month 1:**
- Performance optimization
- Security audit
- Disaster recovery testing
- Cost optimization review

---

## CI/CD Setup (Post-Deployment Goal)

**Why GitHub Actions?**
- ✅ Runs on AMD64 runners (no platform mismatch issues)
- ✅ Automated deployments on `git push`
- ✅ Consistent, repeatable process
- ✅ Secrets management via GitHub Secrets
- ✅ Audit trail for all deployments

### Setup Steps

**1. Create IAM User for GitHub Actions**
```bash
# Create IAM user with programmatic access
aws iam create-user --user-name github-actions-ecs-deploy

# Attach policies for ECR and ECS access
aws iam attach-user-policy \
  --user-name github-actions-ecs-deploy \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

aws iam attach-user-policy \
  --user-name github-actions-ecs-deploy \
  --policy-arn arn:aws:iam::aws:policy/AmazonECS_FullAccess

# Create access key
aws iam create-access-key --user-name github-actions-ecs-deploy
# Save the AccessKeyId and SecretAccessKey!
```

**2. Add GitHub Secrets** (Settings → Secrets and variables → Actions → New repository secret):
- `AWS_ACCESS_KEY_ID` - IAM user access key ID
- `AWS_SECRET_ACCESS_KEY` - IAM user secret access key
- `AWS_REGION` - us-east-1
- `ECR_REPOSITORY` - repo-reconnoiter-prod
- `ECS_CLUSTER` - repo-reconnoiter-prod
- `ECS_SERVICE` - repo-reconnoiter-prod

**3. Create `.github/workflows/deploy.yml`:**
```yaml
name: Deploy to AWS ECS

on:
  push:
    branches: [main]
  workflow_dispatch:  # Allow manual trigger

jobs:
  deploy:
    runs-on: ubuntu-latest  # AMD64, no platform issues!

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag, and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: ${{ secrets.ECR_REPOSITORY }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

      - name: Deploy to ECS
        env:
          ECS_CLUSTER: ${{ secrets.ECS_CLUSTER }}
          ECS_SERVICE: ${{ secrets.ECS_SERVICE }}
        run: |
          aws ecs update-service \
            --cluster $ECS_CLUSTER \
            --service $ECS_SERVICE \
            --force-new-deployment

      - name: Wait for deployment to complete
        env:
          ECS_CLUSTER: ${{ secrets.ECS_CLUSTER }}
          ECS_SERVICE: ${{ secrets.ECS_SERVICE }}
        run: |
          aws ecs wait services-stable \
            --cluster $ECS_CLUSTER \
            --services $ECS_SERVICE
```

**4. Test the workflow:**
- Push a commit to `main` branch
- Go to GitHub Actions tab
- Watch the deployment run
- Verify new tasks started in ECS

### Benefits of CI/CD

✅ **No platform issues** - GitHub runners are AMD64, no need for `--platform` flag
✅ **Faster builds** - Native AMD64, no QEMU emulation
✅ **Git SHA tagging** - Every image tagged with commit SHA for easy rollback
✅ **Automatic deployments** - Push to main = auto deploy
✅ **Zero local setup** - New team members just push code

### Rollback with Git SHA Tags

If a deployment fails, rollback to previous version:
```bash
# List recent images
aws ecr describe-images --repository-name repo-reconnoiter-prod \
  --query 'sort_by(imageDetails,& imagePushedAt)[-10:].[imageTags[0],imagePushedAt]' \
  --output table

# Update ECS to use specific SHA
aws ecs update-service \
  --cluster repo-reconnoiter-prod \
  --service repo-reconnoiter-prod \
  --task-definition repo-reconnoiter-prod \
  --force-new-deployment \
  --override taskDefinition with specific image SHA
```

---

## Troubleshooting Common Issues

### Issue 1: CloudFormation stuck waiting for ECS service

**Symptoms:**
- CDK deployment at 57/63 resources for 90+ minutes
- ECS service shows `DesiredCount: 2, RunningCount: 0`
- Tasks keep failing and restarting

**Diagnosis:**
```bash
# Check ECS service status
aws ecs describe-services \
  --cluster repo-reconnoiter-prod \
  --services repo-reconnoiter-prod \
  --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,Events:events[0:2]}'

# Check why tasks are failing
aws logs tail /ecs/repo-reconnoiter-prod --follow --since 10m
```

**Common Causes:**

**A) Missing Docker image** (CannotPullContainerError)
```
Error: "image Manifest does not contain descriptor matching platform 'linux/amd64'"
```
**Solution:** Build Docker image with correct platform flag:
```bash
docker buildx build --platform linux/amd64 --push \
  -t 626635410245.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter-prod:latest .
```

**B) Missing environment variable** (Application startup failure)
```
Error: "Could not resolve placeholder 'SENTRY_DSN' in value \"${SENTRY_DSN}\""
```
**Solution:** Add missing variable to CDK task definition:
```typescript
environment: {
  SENTRY_DSN: 'https://placeholder@sentry.io/placeholder',
  APP_FRONTEND_URL: 'https://reporeconnoiter.com',
  // ... other vars
}
```

**C) Database connection failure**
```
Error: "Connection refused" or "Unknown host"
```
**Solution:** Check security group allows ECS → RDS on port 3306

### Issue 2: Platform mismatch (Apple Silicon)

**Error:**
```
CannotPullContainerError: image Manifest does not contain descriptor matching platform 'linux/amd64'
```

**Explanation:**
- Docker images built on M-series Macs default to ARM64
- ECS Fargate requires AMD64 (x86_64)
- Docker containers share host kernel, not true VMs

**Solution:**
```bash
# Delete wrong image
aws ecr batch-delete-image \
  --repository-name repo-reconnoiter-prod \
  --image-ids imageTag=latest

# Build for correct platform
docker buildx build --platform linux/amd64 --push \
  -t <ECR_URI>:latest .
```

### Issue 3: ECS tasks in backoff after failures

**Symptoms:**
- ECS shows `RunningCount: 0` with no new tasks starting
- Service events: "unable to consistently start tasks successfully"

**Solution:**
```bash
# Force ECS to retry with new image/config
aws ecs update-service \
  --cluster repo-reconnoiter-prod \
  --service repo-reconnoiter-prod \
  --force-new-deployment
```

### Issue 4: CloudFormation rollback in progress

**Symptoms:**
- Stack status: `ROLLBACK_IN_PROGRESS` or `ROLLBACK_COMPLETE`
- Resources being deleted

**What happened:**
CloudFormation detected deployment failure and is cleaning up.

**Solution:**
```bash
# Wait for rollback to complete
aws cloudformation wait stack-rollback-complete \
  --stack-name RepoReconnoiter-prod

# Delete the failed stack
aws cloudformation delete-stack --stack-name RepoReconnoiter-prod

# Wait for deletion
aws cloudformation wait stack-delete-complete \
  --stack-name RepoReconnoiter-prod

# Fix issues in CDK code, then redeploy
npm run deploy
```

### Issue 5: Viewing container logs

**Check application startup errors:**
```bash
# Tail all recent logs
aws logs tail /ecs/repo-reconnoiter-prod --follow --since 10m

# Filter for errors only
aws logs filter-log-events \
  --log-group-name /ecs/repo-reconnoiter-prod \
  --start-time $(($(date +%s) - 600))000 \
  --filter-pattern "ERROR"
```

### Issue 6: Verifying Docker image architecture

**Check image platform:**
```bash
# Describe image details
aws ecr describe-images \
  --repository-name repo-reconnoiter-prod \
  --image-ids imageTag=latest \
  --query 'imageDetails[0].imageScanFindingsSummary'

# Or inspect locally
docker image inspect repo-reconnoiter:latest | grep Architecture
```

Should show: `"Architecture": "amd64"`

### Quick Diagnostic Commands

```bash
# 1. Check CloudFormation stack status
aws cloudformation describe-stacks \
  --stack-name RepoReconnoiter-prod \
  --query 'Stacks[0].StackStatus'

# 2. Check ECS service health
aws ecs describe-services \
  --cluster repo-reconnoiter-prod \
  --services repo-reconnoiter-prod \
  --query 'services[0].{Running:runningCount,Desired:desiredCount}'

# 3. Check recent task failures
aws ecs list-tasks \
  --cluster repo-reconnoiter-prod \
  --desired-status STOPPED \
  --max-results 1 | grep taskArn

# 4. View application logs
aws logs tail /ecs/repo-reconnoiter-prod --since 5m

# 5. Verify Docker image exists
aws ecr describe-images --repository-name repo-reconnoiter-prod
```

---

Good luck with deployment! 🚀
