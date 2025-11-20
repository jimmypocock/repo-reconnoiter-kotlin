# AWS Deployment Game Plan

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

### Phase 1: Deploy CDK Infrastructure (~10 minutes)
- [ ] Install CDK dependencies: `cd cdk && npm install`
- [ ] Build CDK: `npm run build`
- [ ] Preview changes: `npm run diff -- -c environment=dev`
- [ ] Deploy stack: `npm run deploy -- -c environment=dev`
- [ ] Save outputs (ECR URI, RDS endpoint, Secret ARNs)

### Phase 2: Configure Secrets (~5 minutes)
- [ ] Update GitHub OAuth secret (see commands in CDK README)
- [ ] Update OpenAI API key secret (see commands in CDK README)
- [ ] Verify secrets: `aws secretsmanager list-secrets | grep repo-reconnoiter`

### Phase 3: Build and Push Docker Image (~5 minutes)
- [ ] Authenticate to ECR: `aws ecr get-login-password | docker login...`
- [ ] Build image: `docker build -t repo-reconnoiter .`
- [ ] Tag image: `docker tag repo-reconnoiter:latest ECR_URI:latest`
- [ ] Push image: `docker push ECR_URI:latest`
- [ ] Verify image: `aws ecr list-images --repository-name repo-reconnoiter-dev`

### Phase 4: Run Database Migrations (~5 minutes)
- [ ] Get DB password from Secrets Manager
- [ ] Run Flyway migrations locally (or let ECS run on startup)
- [ ] Verify migrations: Connect to RDS and check `flyway_schema_history`

### Phase 5: Deploy ECS Service (Automatic via CDK)
- [ ] ECS service automatically created by CDK
- [ ] Tasks start and pull Docker image from ECR
- [ ] Health checks pass at `/actuator/health`
- [ ] ALB routes traffic to healthy tasks

### Phase 6: Verify Deployment (~10 minutes)
- [ ] Check ECS service status: `aws ecs describe-services...`
- [ ] Check ECS task health: `aws ecs describe-tasks...`
- [ ] View logs: `aws logs tail /ecs/repo-reconnoiter-dev --follow`
- [ ] Test health endpoint: `curl https://ALB_URL/actuator/health`
- [ ] Test API endpoint: `curl -H "Authorization: Bearer API_KEY" https://ALB_URL/api/v1/repositories`

### Phase 7: Production Readiness (~5 minutes)
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
- Plan for CI/CD pipeline

**Month 1:**
- Performance optimization
- Security audit
- Disaster recovery testing
- Cost optimization review

---

Good luck with deployment! 🚀
