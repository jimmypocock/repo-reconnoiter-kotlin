# RepoReconnoiter Deployment Guide

## Overview

This guide covers deploying the RepoReconnoiter Kotlin/Spring Boot API to AWS using:
- **AWS App Runner** - Serverless container service for the API
- **HTTP API Gateway** - Rate limiting, caching, and custom domains
- **RDS PostgreSQL 17** - Managed database
- **AWS CDK** - Infrastructure as Code
- **GitHub Actions** - CI/CD (optional)

## Architecture

```
Internet
   ↓
API Gateway (HTTP API)
   ├─ Rate limiting (25 req/day per user)
   ├─ Caching (GET endpoints)
   └─ Custom domain (api.reporeconnoiter.com)
   ↓
App Runner (Spring Boot API)
   ├─ Auto-scaling (1-3 instances)
   ├─ Docker container
   └─ 0.25 vCPU, 0.5 GB RAM
   ↓
RDS PostgreSQL 17
   ├─ db.t3.micro
   ├─ 20 GB storage (auto-scaling to 100 GB)
   └─ Automated backups (7 days)
```

## Prerequisites

### 1. AWS Account Setup

1. Create an AWS account (if you don't have one)
2. Install AWS CLI:
   ```bash
   # macOS
   brew install awscli

   # Verify installation
   aws --version
   ```

3. Configure AWS credentials:
   ```bash
   aws configure
   # Enter:
   # - AWS Access Key ID
   # - AWS Secret Access Key
   # - Default region (e.g., us-east-1)
   # - Default output format (json)
   ```

4. Verify credentials:
   ```bash
   aws sts get-caller-identity
   ```

### 2. Install Dependencies

```bash
# Node.js (for CDK)
# Download from https://nodejs.org/ or use brew
brew install node

# AWS CDK CLI
npm install -g aws-cdk

# Docker (for building images)
# Download from https://www.docker.com/products/docker-desktop
```

### 3. Bootstrap CDK (One-Time Setup)

```bash
cdk bootstrap aws://ACCOUNT-ID/us-east-1
```

Replace `ACCOUNT-ID` with your AWS account ID from `aws sts get-caller-identity`.

## Deployment Steps

### Step 1: Deploy Infrastructure with CDK

```bash
cd cdk

# Install dependencies
npm install

# Build TypeScript
npm run build

# Preview changes (optional)
npm run diff -- -c environment=dev

# Deploy to dev environment
npm run deploy -- -c environment=dev
```

**Expected Output:**
```
✅ RepoReconnoiter-dev

Outputs:
RepoReconnoiter-dev.ECRRepositoryUri = 123456789.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter-dev
RepoReconnoiter-dev.DatabaseEndpoint = repo-reconnoiter-dev-db.xxxxx.us-east-1.rds.amazonaws.com
RepoReconnoiter-dev.DatabaseSecretArn = arn:aws:secretsmanager:us-east-1:123456789:secret:repo-reconnoiter-dev-db-credentials
```

**Save these outputs** - you'll need them for the next steps.

### Step 2: Update Secrets

#### GitHub OAuth Credentials

1. Create GitHub OAuth App:
   - Go to GitHub Settings → Developer Settings → OAuth Apps
   - Click "New OAuth App"
   - Fill in:
     - Application name: RepoReconnoiter Dev
     - Homepage URL: https://api.reporeconnoiter.com
     - Authorization callback URL: https://api.reporeconnoiter.com/login/oauth2/code/github
   - Save Client ID and Client Secret

2. Update AWS Secrets Manager:
   ```bash
   aws secretsmanager update-secret \
     --secret-id repo-reconnoiter-dev-github-oauth \
     --secret-string '{"clientId":"YOUR_CLIENT_ID","clientSecret":"YOUR_CLIENT_SECRET"}'
   ```

#### OpenAI API Key

```bash
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-dev-openai-key \
  --secret-string '{"apiKey":"YOUR_OPENAI_API_KEY"}'
```

#### Verify Secrets

```bash
aws secretsmanager list-secrets | grep repo-reconnoiter
```

### Step 3: Build and Push Docker Image

From the **project root directory**:

```bash
# Get ECR repository URI from CDK output
ECR_URI="<YOUR_ECR_REPOSITORY_URI>"

# Log in to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_URI

# Build Docker image
docker build -t repo-reconnoiter .

# Tag image
docker tag repo-reconnoiter:latest $ECR_URI:latest

# Push to ECR
docker push $ECR_URI:latest
```

**Verify image in ECR:**
```bash
aws ecr list-images --repository-name repo-reconnoiter-dev
```

### Step 4: Run Database Migrations

Option 1: Run migrations locally (for initial setup):

```bash
# Set environment variables
export DATABASE_URL="jdbc:postgresql://DB_ENDPOINT:5432/reconnoiter"
export DATABASE_USERNAME="reconnoiter"
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id repo-reconnoiter-dev-db-credentials \
  --query SecretString --output text | jq -r .password)

# Run migrations
./gradlew flywayMigrate
```

Option 2: Let App Runner run migrations on first startup (automatic).

### Step 5: Create App Runner Service

#### Via AWS Console (Recommended for first deployment)

1. Go to AWS Console → App Runner
2. Click "Create service"
3. **Source:**
   - Repository type: "Container registry"
   - Provider: "Amazon ECR"
   - Container image URI: Select your ECR image
   - Deployment trigger: "Manual" (change to "Automatic" later)
   - ECR access role: Use existing role created by CDK
4. **Service settings:**
   - Service name: `repo-reconnoiter-dev`
   - Virtual CPU: 0.25 vCPU
   - Memory: 0.5 GB
   - Port: 8080
5. **Environment variables:**
   ```
   SPRING_PROFILES_ACTIVE=prod
   SERVER_PORT=8080
   DATABASE_URL=jdbc:postgresql://DB_ENDPOINT:5432/reconnoiter
   DATABASE_USERNAME=reconnoiter
   ```

6. **Secrets (from Secrets Manager):**
   - `DATABASE_PASSWORD` → `repo-reconnoiter-dev-db-credentials:password`
   - `JWT_SECRET` → `repo-reconnoiter-dev-jwt-secret:secret`
   - `GITHUB_CLIENT_ID` → `repo-reconnoiter-dev-github-oauth:clientId`
   - `GITHUB_CLIENT_SECRET` → `repo-reconnoiter-dev-github-oauth:clientSecret`
   - `OPENAI_ACCESS_TOKEN` → `repo-reconnoiter-dev-openai-key:apiKey`

7. **Health check:**
   - Path: `/actuator/health`
   - Interval: 30 seconds
   - Timeout: 5 seconds
   - Unhealthy threshold: 3

8. **Auto scaling:**
   - Min instances: 1
   - Max instances: 3
   - Max concurrency: 100

9. **Networking:**
   - VPC connector: Select the one created by CDK
   - Security groups: Select the database security group

10. **Review and create**

#### Verify Deployment

```bash
# Get App Runner service URL
aws apprunner list-services

# Test health endpoint
curl https://YOUR_APP_RUNNER_URL/actuator/health

# Should return:
# {"status":"UP"}
```

### Step 6: Set Up API Gateway (Optional)

For rate limiting, caching, and custom domain:

1. Go to AWS Console → API Gateway
2. Create "HTTP API"
3. Add integration:
   - Type: HTTP proxy
   - URL: Your App Runner service URL
4. Configure routes:
   - `GET /api/v1/{proxy+}` → App Runner
   - `POST /api/v1/auth/exchange` → App Runner
5. Configure throttling:
   - Default route throttle: 100 req/sec
   - Per-client throttle: 25 req/day (requires usage plans)
6. Enable caching (optional):
   - Cache GET responses for 5 minutes
7. Add custom domain (optional):
   - Domain: api.reporeconnoiter.com
   - Create ACM certificate
   - Update Route 53

### Step 7: Test API

```bash
# Test root endpoint
curl https://YOUR_APP_RUNNER_URL/api/v1/

# Should return API information
{
  "message": "Welcome to RepoReconnoiter API v1",
  "version": "v1",
  ...
}

# Test repositories endpoint (requires API key)
curl -H "Authorization: Bearer YOUR_API_KEY" \
  https://YOUR_APP_RUNNER_URL/api/v1/repositories
```

### Step 8: Generate API Keys

Use the shell to generate API keys:

```bash
# Run shell in production (via App Runner console)
./gradlew shell

# In shell:
shell:> apikey generate "Production API Key"
```

Or create via database directly:

```bash
# Connect to RDS
psql -h DB_ENDPOINT -U reconnoiter -d reconnoiter

# Insert API key (hash it first using BCrypt)
INSERT INTO api_keys (name, key_hash, prefix, created_at, updated_at)
VALUES ('Production', 'BCRYPT_HASH', 'rr_', NOW(), NOW());
```

## Production Deployment

For production, use the same steps but with `environment=prod`:

```bash
# Deploy infrastructure
cd cdk
npm run deploy -- -c environment=prod

# Push Docker image to production ECR
ECR_URI_PROD="<PROD_ECR_URI>"
docker tag repo-reconnoiter:latest $ECR_URI_PROD:latest
docker push $ECR_URI_PROD:latest

# Create App Runner service (via Console)
# Use production secrets and multi-AZ RDS
```

**Production differences:**
- Multi-AZ RDS for high availability
- Deletion protection enabled
- Longer backup retention (7 days)
- Auto-scaling: 2-5 instances

## Continuous Deployment (Optional)

### GitHub Actions

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1

      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: repo-reconnoiter-dev
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
```

## Monitoring

### CloudWatch Logs

```bash
# View App Runner logs
aws logs tail /aws/apprunner/repo-reconnoiter-dev --follow

# View RDS logs
aws rds describe-db-log-files --db-instance-identifier repo-reconnoiter-dev-db
```

### Metrics

Go to AWS Console → CloudWatch → Metrics:
- App Runner: Request count, response time, error rate
- RDS: CPU utilization, connections, IOPS
- API Gateway: Request count, latency, errors

## Cost Optimization

**Development (~$30-45/month):**
- RDS Single-AZ db.t3.micro: ~$15/month
- App Runner (0.25 vCPU, 0.5 GB): ~$15-25/month
- NAT Gateway: ~$32/month (can be removed)
- API Gateway: ~$1-5/month

**Production (~$60-100/month):**
- RDS Multi-AZ db.t3.micro: ~$30/month
- App Runner: ~$25-40/month
- NAT Gateway: ~$32/month
- Other: ~$5-10/month

**Reduce costs:**
1. Remove NAT Gateway in dev (use VPC endpoints)
2. Use Single-AZ RDS in dev
3. Scale to zero when not in use
4. Use reserved instances for predictable workloads

## Troubleshooting

### App Runner service fails to start

**Check logs:**
```bash
aws logs tail /aws/apprunner/repo-reconnoiter-dev --follow
```

**Common issues:**
- Missing environment variables
- Database connection failure
- Invalid secrets

### Database connection fails

**Verify security groups:**
```bash
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=repo-reconnoiter-dev-db-sg"
```

**Test connection:**
```bash
psql -h DB_ENDPOINT -U reconnoiter -d reconnoiter
```

### API returns 500 errors

**Check App Runner logs:**
```bash
aws logs tail /aws/apprunner/repo-reconnoiter-dev --follow
```

**Common issues:**
- Missing API keys
- Flyway migration failures
- Invalid JWT secret

## Rollback

If deployment fails:

```bash
# Rollback infrastructure
cd cdk
npm run destroy -- -c environment=dev

# Delete ECR images
aws ecr batch-delete-image \
  --repository-name repo-reconnoiter-dev \
  --image-ids imageTag=latest
```

## Next Steps

1. **Set up monitoring alerts** - CloudWatch alarms for errors
2. **Configure backups** - Automated RDS snapshots
3. **Add WAF** - Web Application Firewall for security
4. **Custom domain** - Route 53 + ACM certificate
5. **CI/CD** - GitHub Actions for automated deployments

## Support

For issues or questions:
- Check CloudWatch logs
- Review AWS documentation
- Open GitHub issue
