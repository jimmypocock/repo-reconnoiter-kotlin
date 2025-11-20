# RepoReconnoiter AWS CDK Infrastructure

This directory contains AWS CDK infrastructure code for deploying RepoReconnoiter to AWS using **ECS Fargate**.

## Architecture

- **VPC**: Multi-AZ VPC with public, private, and isolated database subnets
- **RDS MySQL 8.0**: Managed database with automated backups and encryption
- **ECS Fargate**: Serverless container orchestration (to be added)
- **Application Load Balancer**: Public-facing load balancer for HTTPS traffic (to be added)
- **Secrets Manager**: Secure storage for sensitive configuration
- **ECR**: Container registry for Docker images
- **CloudWatch**: Logging and monitoring

## Current Status

✅ **Completed:**
- VPC with proper subnet structure
- RDS MySQL 8.0 database
- ECR repository
- Secrets Manager for all credentials
- Security groups

🚧 **To Be Added:**
- ECS Fargate cluster
- Task Definition with container configuration
- ECS Service with auto-scaling
- Application Load Balancer with target groups
- CloudWatch Log Groups

See [DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md) for the complete deployment plan.

## Prerequisites

1. **AWS CLI** configured with credentials:
   ```bash
   aws configure
   ```

2. **Node.js 18+** and **npm**:
   ```bash
   node --version
   npm --version
   ```

3. **AWS CDK CLI**:
   ```bash
   npm install -g aws-cdk
   ```

4. **Bootstrap your AWS account** (one-time setup):
   ```bash
   cdk bootstrap aws://ACCOUNT-ID/REGION
   ```

## Installation

1. Install dependencies:
   ```bash
   cd cdk
   npm install
   ```

2. Build the TypeScript code:
   ```bash
   npm run build
   ```

## Deployment

### Phase 1: Deploy Infrastructure

Deploy to development environment:
```bash
npm run deploy -- -c environment=dev
```

Deploy to production environment:
```bash
npm run deploy -- -c environment=prod
```

The deployment will output:
- ECR Repository URI
- RDS MySQL endpoint and port
- Secret ARNs for database, JWT, GitHub OAuth, OpenAI

### Phase 2: Update Secrets

After deployment, update the placeholder secrets via AWS Console or CLI:

**GitHub OAuth Credentials:**
```bash
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-dev-github-oauth \
  --secret-string '{"clientId":"YOUR_GITHUB_CLIENT_ID","clientSecret":"YOUR_GITHUB_CLIENT_SECRET"}'
```

**OpenAI API Key:**
```bash
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-dev-openai-key \
  --secret-string '{"apiKey":"YOUR_OPENAI_API_KEY"}'
```

### Phase 3: Build and Push Docker Image

From the **project root directory** (not the cdk directory):

```bash
# Get ECR login credentials
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <ECR_REPOSITORY_URI>

# Build Docker image
docker build -t repo-reconnoiter .

# Tag image for ECR
docker tag repo-reconnoiter:latest <ECR_REPOSITORY_URI>:latest

# Push to ECR
docker push <ECR_REPOSITORY_URI>:latest
```

Replace `<ECR_REPOSITORY_URI>` with the output from the CDK deployment (e.g., `123456789.dkr.ecr.us-east-1.amazonaws.com/repo-reconnoiter-dev`).

### Phase 4: Run Database Migrations

Option 1 - Run locally (for initial setup):
```bash
# Set environment variables
export DATABASE_URL="jdbc:mysql://DB_ENDPOINT:3306/reconnoiter"
export DATABASE_USERNAME="reconnoiter"
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id repo-reconnoiter-dev-db-credentials \
  --query SecretString --output text | jq -r .password)

# Run Flyway migrations
./gradlew flywayMigrate
```

Option 2 - Let ECS run migrations on startup (automatic).

### Phase 5: Deploy ECS Service (Coming Soon)

Once ECS infrastructure is added to the CDK stack, this will create:
- ECS Fargate cluster
- Task Definition with container configuration
- ECS Service with auto-scaling
- Application Load Balancer with HTTPS

## Useful CDK Commands

- `npm run build` - Compile TypeScript to JavaScript
- `npm run watch` - Watch for changes and recompile
- `npm run diff` - Compare deployed stack with current state
- `npm run deploy` - Deploy stack to AWS
- `npm run destroy` - Destroy stack (use with caution!)
- `cdk synth` - Generate CloudFormation template

## Cost Estimate

**Development environment (~$30/month):**
- RDS Single-AZ db.t3.micro: ~$15/month
- ECS Fargate (0.25 vCPU, 0.5GB): ~$15/month
- NAT Gateway: ~$32/month (can be removed with VPC endpoints)
- ALB: ~$20/month

**Production environment (~$87/month):**
- RDS Multi-AZ db.t3.micro: ~$30/month
- ECS Fargate: ~$15-25/month
- NAT Gateway: ~$32/month
- ALB: ~$20/month

**Cost optimization tips:**
- Use VPC endpoints to avoid NAT Gateway ($32 savings)
- Use Single-AZ RDS in dev
- Use RDS reserved instances (save 40%)
- Scale down ECS task count during off-hours

## Environment Variables

The following environment variables are required for the application:

**From Secrets Manager:**
- `DATABASE_PASSWORD` - Database password (auto-generated)
- `JWT_SECRET` - JWT signing secret (auto-generated)
- `GITHUB_CLIENT_ID` - GitHub OAuth client ID (must set manually)
- `GITHUB_CLIENT_SECRET` - GitHub OAuth client secret (must set manually)
- `OPENAI_ACCESS_TOKEN` - OpenAI API key (must set manually)

**Direct Environment Variables:**
- `SPRING_PROFILES_ACTIVE` - Set to "prod"
- `DATABASE_URL` - jdbc:mysql://DB_ENDPOINT:3306/reconnoiter
- `DATABASE_USERNAME` - reconnoiter
- `SERVER_PORT` - 8080
- `APP_FRONTEND_URL` - Frontend URL for CORS

## Security

- All secrets stored in AWS Secrets Manager with encryption
- Database in private isolated subnet with no public access
- Security groups restrict access to necessary ports only
- ECS tasks in private subnet with NAT Gateway for outbound access
- IAM roles follow principle of least privilege
- RDS encrypted at rest with automated backups

## Monitoring

CloudWatch logs will be configured for:
- RDS MySQL logs (error, general, slowquery)
- ECS task logs (application stdout/stderr)
- ALB access logs

Access logs (once ECS is deployed):
```bash
aws logs tail /ecs/repo-reconnoiter-dev --follow
```

## Troubleshooting

**CDK deployment fails:**
- Check AWS credentials: `aws sts get-caller-identity`
- Verify CDK bootstrap: `cdk bootstrap`
- Check for conflicting resource names
- Ensure TypeScript compiles: `npm run build`

**Database connection fails:**
- Check security groups allow ECS → RDS on port 3306
- Verify VPC subnet configuration
- Confirm database credentials in Secrets Manager

**Docker image push fails:**
- Check ECR login: `aws ecr get-login-password`
- Verify repository exists: `aws ecr describe-repositories`
- Check IAM permissions for ECR

## Further Reading

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/v2/guide/home.html)
- [AWS ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [AWS RDS MySQL Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MySQL.html)
