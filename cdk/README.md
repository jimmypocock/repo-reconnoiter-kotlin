# RepoReconnoiter AWS CDK Infrastructure

This directory contains AWS CDK infrastructure code for deploying RepoReconnoiter to AWS.

## Architecture

- **VPC**: Multi-AZ VPC with public, private, and database subnets
- **RDS PostgreSQL 17**: Managed database with automated backups
- **AWS App Runner**: Serverless container service for the API
- **HTTP API Gateway**: API Gateway for rate limiting, caching, and custom domains
- **Secrets Manager**: Secure storage for sensitive configuration
- **ECR**: Container registry for Docker images
- **CloudWatch**: Logging and monitoring

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

### 1. Deploy Infrastructure

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
- Database endpoint and credentials
- Secret ARNs

### 2. Update Secrets

After deployment, update the secrets via AWS Console or CLI:

**GitHub OAuth Credentials:**
```bash
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-dev-github-oauth \
  --secret-string '{"clientId":"your_github_client_id","clientSecret":"your_github_client_secret"}'
```

**OpenAI API Key:**
```bash
aws secretsmanager update-secret \
  --secret-id repo-reconnoiter-dev-openai-key \
  --secret-string '{"apiKey":"your_openai_api_key"}'
```

### 3. Build and Push Docker Image

From the project root directory:

```bash
# Get ECR login
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_REPOSITORY_URI>

# Build Docker image
docker build -t repo-reconnoiter .

# Tag image
docker tag repo-reconnoiter:latest <ECR_REPOSITORY_URI>:latest

# Push to ECR
docker push <ECR_REPOSITORY_URI>:latest
```

Replace `<ECR_REPOSITORY_URI>` with the output from the CDK deployment.

### 4. Create App Runner Service

After pushing your Docker image to ECR, create the App Runner service via AWS Console or CLI:

**Via AWS Console:**
1. Go to AWS App Runner
2. Click "Create service"
3. Select "Container registry" → "Amazon ECR"
4. Select your ECR repository and tag
5. Configure service settings:
   - Port: 8080
   - Environment variables (from Secrets Manager)
   - VPC connector (select the one created by CDK)
   - Auto-scaling: 1-3 instances
6. Review and create

**Via CLI:**
```bash
# See AWS documentation for App Runner CLI commands
# https://docs.aws.amazon.com/apprunner/latest/api/Welcome.html
```

### 5. Set Up API Gateway (Optional)

If you want to add rate limiting, caching, and custom domain:

1. Create HTTP API Gateway
2. Add integration to App Runner service URL
3. Configure throttling settings
4. Set up caching for GET endpoints
5. Add custom domain name

## Useful CDK Commands

- `npm run build` - Compile TypeScript to JavaScript
- `npm run watch` - Watch for changes and recompile
- `npm run diff` - Compare deployed stack with current state
- `npm run deploy` - Deploy stack to AWS
- `npm run destroy` - Destroy stack (use with caution!)
- `cdk synth` - Generate CloudFormation template

## Cost Estimate

**Development environment (~$30-45/month):**
- RDS db.t3.micro: ~$15/month
- App Runner (0.25 vCPU, 0.5GB): ~$15-25/month
- NAT Gateway: ~$32/month (can be removed for dev)
- HTTP API Gateway: ~$1-5/month
- Data transfer: ~$1-5/month

**Production environment (~$60-100/month):**
- RDS Multi-AZ db.t3.micro: ~$30/month
- App Runner: ~$25-40/month
- NAT Gateway: ~$32/month
- Other services: ~$5-10/month

**Cost optimization tips:**
- Remove NAT Gateway in dev (use VPC endpoints instead)
- Use Single-AZ RDS in dev
- Set up auto-scaling to scale to zero when not in use
- Use reserved instances for predictable workloads

## Environment Variables

The following environment variables are set via Secrets Manager:

- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password (from Secrets Manager)
- `GITHUB_CLIENT_ID` - GitHub OAuth client ID
- `GITHUB_CLIENT_SECRET` - GitHub OAuth client secret
- `OPENAI_ACCESS_TOKEN` - OpenAI API key
- `JWT_SECRET` - JWT signing secret
- `SPRING_PROFILES_ACTIVE` - Set to "prod"

## Security

- All secrets stored in AWS Secrets Manager
- Database in private subnet with no public access
- Security groups restrict access to necessary ports only
- IAM roles follow principle of least privilege
- Encrypted storage for RDS
- Regular automated backups

## Monitoring

CloudWatch logs are automatically configured for:
- RDS PostgreSQL logs
- App Runner application logs
- API Gateway access logs (if configured)

Access logs:
```bash
aws logs tail /aws/apprunner/repo-reconnoiter-dev --follow
```

## Troubleshooting

**CDK deployment fails:**
- Check AWS credentials: `aws sts get-caller-identity`
- Verify CDK bootstrap: `cdk bootstrap`
- Check for conflicting resource names

**App Runner service fails to start:**
- Check CloudWatch logs
- Verify all secrets are set correctly
- Ensure Docker image is in ECR
- Verify VPC connector configuration

**Database connection fails:**
- Check security groups
- Verify VPC connector is attached to App Runner
- Confirm database credentials in Secrets Manager

## Further Reading

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/v2/guide/home.html)
- [AWS App Runner Documentation](https://docs.aws.amazon.com/apprunner/latest/dg/what-is-apprunner.html)
- [HTTP API Gateway Documentation](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html)
