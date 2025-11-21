# RepoReconnoiter AWS CDK Infrastructure

AWS CDK infrastructure for deploying RepoReconnoiter Kotlin API to **ECS Fargate** with **RDS MySQL**.

## Current Status

✅ **Production Deployment LIVE**
- API URL: `http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com`
- Health: `http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com/api/v1/actuator/health`
- OpenAPI: `http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com/api/v1/openapi.yml`

## Architecture

### Deployed Infrastructure

**Network Stack** (`RepoReconnoiter-Network-prod`):
- VPC with 2 Availability Zones
- Public subnets (for ALB)
- Private subnets with NAT Gateway (for ECS tasks)
- Isolated subnets (for RDS database)

**Security Groups Stack** (`RepoReconnoiter-SecurityGroups-prod`):
- ALB Security Group: Allows HTTP/HTTPS from internet
- ECS Security Group: Allows traffic from ALB on port 8080
- RDS Security Group: Allows MySQL (3306) from ECS only

**Database Stack** (`RepoReconnoiter-Database-prod`):
- RDS MySQL 8.0 (db.t3.micro)
- 20GB GP3 storage with auto-scaling to 100GB
- 7-day automated backups
- CloudWatch logs enabled (error, general, slowquery)
- SNS topic for low storage alarm (<2GB remaining)
- Located in isolated subnets (no internet access)

**API Stack** (`RepoReconnoiter-API-prod`):
- ECR Repository with image scanning and lifecycle rules (keep last 10 images)
- Secrets Manager for JWT, GitHub OAuth, OpenAI API key
- ECS Fargate Cluster with Container Insights (enhanced observability)
- ECS Task Definition (0.5 vCPU, 1GB RAM)
- ECS Service with auto-scaling (2-5 tasks based on CPU/memory at 70%)
- Application Load Balancer with HTTP listener (HTTPS optional via ACM certificate)
- CloudWatch Logs with 7-day retention
- SNS topic for zero-tasks alarm (critical outage detection)

### Cost Estimate

**Production environment (~$50/month):**
- RDS Single-AZ db.t3.micro: ~$15/month
- ECS Fargate (0.5 vCPU, 1GB, 2 tasks): ~$15/month
- NAT Gateway: ~$32/month
- ALB: ~$20/month
- **Total: ~$82/month**

**Cost optimization:**
- Single-AZ RDS for dev/staging (Multi-AZ adds ~$15/month)
- NAT Gateway can be replaced with VPC endpoints (-$32/month)
- Use RDS reserved instances (save 40%)
- Scale down ECS during off-hours

## Prerequisites

1. **AWS CLI** configured:
   ```bash
   aws configure
   aws sts get-caller-identity  # Verify credentials
   ```

2. **Node.js 18+** and npm:
   ```bash
   node --version  # Should be 18+
   ```

3. **AWS CDK CLI**:
   ```bash
   npm install -g aws-cdk
   cdk --version
   ```

4. **CDK Bootstrap** (one-time per account/region):
   ```bash
   cdk bootstrap aws://ACCOUNT-ID/us-east-1
   ```

## Installation

```bash
cd cdk

# Copy environment config
cp .env.example .env
# Edit .env: Set AWS_PROFILE, AWS_REGION, ENVIRONMENT

# Install dependencies
npm install

# Build TypeScript
npm run build
```

## Deployment

### Professional Two-Phase Deployment Workflow

The code is **production-ready by default** (`desiredCount: 2`), but supports a context variable for fresh deployments when no Docker image exists yet.

#### Phase 1: Fresh Deployment (No Docker Image Yet)

```bash
# 1. Deploy all infrastructure with 0 ECS tasks
npm run deploy:all

# OR deploy individually:
npm run deploy:network
npm run deploy:security-groups
npm run deploy:database
npm run deploy:api:fresh  # Uses --context desiredCount=0

# Save outputs: ECR URI, RDS endpoint, ALB URL
```

#### Phase 2: Build and Push Docker Image

```bash
# Go to project root (not cdk/)
cd ..

# Build for ECS Fargate (requires linux/amd64)
docker build --platform linux/amd64 -t repo-reconnoiter:latest .

# Tag for ECR (get URI from CloudFormation outputs)
docker tag repo-reconnoiter:latest <ECR_URI>:latest
docker tag repo-reconnoiter:latest <ECR_URI>:v1.0.0

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <ECR_URI>

# Push to ECR
docker push <ECR_URI>:latest
docker push <ECR_URI>:v1.0.0
```

**Note for Apple Silicon (M1/M2/M3):**
- Must use `--platform linux/amd64` (ECS Fargate requires x86_64)
- Uses QEMU emulation (slower but required)
- Production CI/CD (GitHub Actions) runs on AMD64 natively (faster)

#### Phase 3: Deploy with Tasks

```bash
cd cdk

# Deploy with production-ready defaults (desiredCount: 2)
npm run deploy:api

# CloudFormation will:
# 1. Update task definition
# 2. Create new tasks that pull latest image from ECR
# 3. Wait for tasks to pass health checks
# 4. Register tasks with ALB target group
# 5. Complete deployment when tasks are HEALTHY
```

### Normal Deployments (After Initial Setup)

For code changes, just redeploy - the code is production-ready:

```bash
# Update code, rebuild Docker image, push to ECR
docker build --platform linux/amd64 -t repo-reconnoiter:latest .
docker tag repo-reconnoiter:latest <ECR_URI>:latest
docker push <ECR_URI>:latest

# Redeploy API stack (uses desiredCount: 2 by default)
cd cdk
npm run deploy:api
```

### Available npm Scripts

```bash
npm run build                    # Compile TypeScript
npm run watch                    # Watch mode for development
npm run diff                     # Show changes before deployment
npm run deploy                   # Deploy all stacks
npm run deploy:network           # Deploy Network stack only
npm run deploy:security-groups   # Deploy SecurityGroups stack only
npm run deploy:database          # Deploy Database stack only
npm run deploy:api               # Deploy API stack (desiredCount: 2)
npm run deploy:api:fresh         # Deploy API stack (desiredCount: 0)
npm run deploy:all               # Deploy all stacks in order
npm run destroy                  # Destroy all stacks (use with caution!)
npm run synth                    # Generate CloudFormation template
```

## Configuration

### Environment Variables (.env)

```bash
# AWS Configuration
AWS_PROFILE=your-sso-profile    # AWS SSO profile name
AWS_REGION=us-east-1            # AWS region
ENVIRONMENT=prod                # Environment name (prod/staging/dev)
AWS_ACCOUNT_ID=123456789012     # Your AWS account ID

# Optional: ACM Certificate ARN for HTTPS
ACM_CERTIFICATE_ARN=arn:aws:acm:us-east-1:123456789012:certificate/...
```

### Application Secrets

The CDK creates secrets in AWS Secrets Manager with placeholder values. Update them after deployment:

```bash
# GitHub OAuth Credentials
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-prod-github-oauth \
  --secret-string '{"clientId":"YOUR_GITHUB_CLIENT_ID","clientSecret":"YOUR_GITHUB_CLIENT_SECRET"}'

# OpenAI API Key
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-prod-openai-key \
  --secret-string '{"apiKey":"YOUR_OPENAI_API_KEY"}'

# JWT Secret and DB Password are auto-generated - no action needed
```

### Health Check Configuration

**Critical:** Spring Boot uses context path `/api/v1`, so health checks must include this path:

- **ALB Target Group**: `/api/v1/actuator/health`
- **Container Health Check**: `curl -f http://localhost:8080/api/v1/actuator/health`

**Common mistake:** Using `/actuator/health` (wrong) instead of `/api/v1/actuator/health` (correct) will cause 404 errors and unhealthy tasks.

## Monitoring

### CloudWatch Logs

```bash
# View ECS task logs (real-time)
aws logs tail /ecs/repo-reconnoiter-prod --follow

# View logs from last 10 minutes
aws logs tail /ecs/repo-reconnoiter-prod --since 10m

# Filter for errors
aws logs filter-log-events \
  --log-group-name /ecs/repo-reconnoiter-prod \
  --filter-pattern "ERROR"
```

### CloudWatch Alarms

**Zero Tasks Alarm** (Critical):
- Triggers when `RunningTaskCount < 1` for 2 consecutive minutes
- Sends email notification via SNS
- Indicates complete application outage

**Database Low Storage Alarm** (Warning):
- Triggers when free storage < 2GB
- Sends email notification via SNS
- Action required: Increase RDS storage allocation

**SNS Email Subscriptions:**
- Check email for "AWS Notification - Subscription Confirmation"
- Click confirmation link to receive alarm notifications

### ECS Service Status

```bash
# Check ECS service health
aws ecs describe-services \
  --cluster repo-reconnoiter-prod \
  --services repo-reconnoiter-prod \
  --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}'

# Check task health
aws ecs list-tasks --cluster repo-reconnoiter-prod --service-name repo-reconnoiter-prod
aws ecs describe-tasks --cluster repo-reconnoiter-prod --tasks <TASK_ARN>
```

### ALB Health Checks

```bash
# Test health endpoint directly
curl http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com/api/v1/actuator/health

# Expected response:
{"status":"UP","groups":["liveness","readiness"]}
```

## Database Access

### Option 1: ECS Exec (Recommended)

Execute commands inside running ECS container:

```bash
# Get running task ID
aws ecs list-tasks --cluster repo-reconnoiter-prod --service-name repo-reconnoiter-prod

# Connect to container
aws ecs execute-command \
  --cluster repo-reconnoiter-prod \
  --task <TASK_ID> \
  --container repo-reconnoiter-api \
  --interactive \
  --command "/bin/bash"

# Inside container:
mysql -h <RDS_ENDPOINT> -u reconnoiter -p
```

### Option 2: Direct MySQL Connection

Temporarily add your IP to RDS security group, then:

```bash
# Get RDS endpoint from CloudFormation outputs
mysql -h <RDS_ENDPOINT> -u reconnoiter -p

# Get password from Secrets Manager:
aws secretsmanager get-secret-value \
  --secret-id repo-reconnoiter-prod-db-credentials \
  --query SecretString --output text | jq -r .password
```

### Database Seeding

**Flyway Migration (Recommended):**

```bash
# 1. Create seed migration file
# File: src/main/resources/db/migration/V17__seed_categories.sql
INSERT INTO categories (name, category_type, description, created_at, updated_at) VALUES
('Ruby', 'LANGUAGE', 'Ruby programming language', NOW(), NOW()),
('Python', 'LANGUAGE', 'Python programming language', NOW(), NOW());

# 2. Rebuild and redeploy - Flyway runs on container startup
docker build --platform linux/amd64 -t repo-reconnoiter:latest .
docker push <ECR_URI>:latest
cd cdk && npm run deploy:api
```

**Direct SQL (For Testing):**

```bash
# Connect via ECS Exec or direct MySQL connection, then:
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE categories;
INSERT INTO categories (name, category_type, description, created_at, updated_at) VALUES
('Ruby', 'LANGUAGE', 'Ruby programming language', NOW(), NOW());
SET FOREIGN_KEY_CHECKS = 1;
```

### Running Gradle Tasks in Production (ECS Exec)

The Docker image includes Gradle and JDK for admin tasks. Run Gradle tasks via ECS Exec with memory limits to prevent OOM kills.

**Prerequisites:**
- Container size: 2GB RAM minimum (configured in `cdk/lib/api-stack.ts`)
- Spring Boot app runs alongside Gradle (~400MB)
- Gradle needs limited heap to fit within container

**Steps:**

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

# Generate API key
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  apiKeyGenerate -Pname="Production Key"

# Seed database with categories from JSON files
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  dbSeed

# List API keys
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m" \
  apiKeyList

# Exit container
exit
```

**Memory breakdown:**
- Spring Boot app: ~400 MB
- Gradle with limits: ~768 MB
- Total: ~1.2 GB (within 2GB container limit)

**Why these flags are required:**
- `--no-daemon`: Prevents Gradle daemon (saves memory)
- `-Xmx768m`: Limits Java heap to 768MB (default is 2GB, which exceeds container limit)
- `-XX:MaxMetaspaceSize=256m`: Limits metadata memory

**Without these limits**, Gradle will try to allocate 2GB heap and be killed by ECS for exceeding the container's 2GB memory limit.

## Troubleshooting

### ECS Tasks Failing Health Checks

**Symptoms:** Tasks stuck in UNHEALTHY state, ALB returning 404

**Diagnosis:**
```bash
# Check task logs
aws logs tail /ecs/repo-reconnoiter-prod --since 10m

# Check ALB target health
aws elbv2 describe-target-health --target-group-arn <TARGET_GROUP_ARN>
```

**Common causes:**
1. Wrong health check path (must be `/api/v1/actuator/health`, not `/actuator/health`)
2. Application startup failure (check CloudWatch logs)
3. Security group blocking ALB → ECS traffic on port 8080

### CloudFormation Deployment Stuck

**Symptoms:** Stack stuck in `UPDATE_IN_PROGRESS` for 10+ minutes

**Diagnosis:**
```bash
# Check CloudFormation events
aws cloudformation describe-stack-events \
  --stack-name RepoReconnoiter-API-prod \
  --max-items 20 \
  --query 'StackEvents[*].{Time:Timestamp,Status:ResourceStatus,Reason:ResourceStatusReason}'

# Check ECS service events
aws ecs describe-services \
  --cluster repo-reconnoiter-prod \
  --services repo-reconnoiter-prod \
  --query 'services[0].events[0:5]'
```

**Solution:** Cancel stuck update and redeploy after fixing issues:
```bash
aws cloudformation cancel-update-stack --stack-name RepoReconnoiter-API-prod
```

### Docker Platform Mismatch

**Error:** `CannotPullContainerError: image Manifest does not contain descriptor matching platform 'linux/amd64'`

**Cause:** Docker image built for ARM64 (Apple Silicon) instead of AMD64 (ECS Fargate)

**Solution:**
```bash
# Delete wrong image
aws ecr batch-delete-image \
  --repository-name repo-reconnoiter-prod \
  --image-ids imageTag=latest

# Rebuild for correct platform
docker buildx build --platform linux/amd64 --push \
  -t <ECR_URI>:latest .
```

### SNS Topic Drift Warning

**Symptoms:** CloudWatch alarm shows "We could not find the SNS topic for this action"

**Cause:** SNS topic was manually deleted but CloudFormation state not updated

**Solution:**
```bash
# Recreate topic matching CloudFormation's expected ARN
aws sns create-topic --name repo-reconnoiter-prod-alarms

# Resubscribe email
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:ACCOUNT:repo-reconnoiter-prod-alarms \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## Security

- All secrets encrypted in AWS Secrets Manager
- Database in private isolated subnet (no internet access)
- ECS tasks in private subnet with NAT Gateway for outbound only
- Security groups follow principle of least privilege
- RDS encrypted at rest with automated backups
- IAM roles use minimum required permissions
- Container runs as non-root user

## Clean Up

**Warning:** This will delete all infrastructure and data (except RDS snapshots if enabled)

```bash
# Delete all stacks in reverse order
aws cloudformation delete-stack --stack-name RepoReconnoiter-API-prod
aws cloudformation delete-stack --stack-name RepoReconnoiter-Database-prod
aws cloudformation delete-stack --stack-name RepoReconnoiter-SecurityGroups-prod
aws cloudformation delete-stack --stack-name RepoReconnoiter-Network-prod

# Or use CDK destroy (interactive)
npm run destroy
```

## Further Reading

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/v2/guide/home.html)
- [AWS ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [AWS RDS MySQL Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MySQL.html)
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
