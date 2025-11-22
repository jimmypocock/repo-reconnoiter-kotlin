# GitHub Actions CI/CD Setup Guide

This guide walks through setting up automated deployments to AWS ECS using GitHub Actions.

## Step 1: Create IAM User for GitHub Actions

Create a dedicated IAM user with minimal required permissions:

```bash
# Create IAM user
aws iam create-user --user-name github-actions-repo-reconnoiter

# Create access key (save the output!)
aws iam create-access-key --user-name github-actions-repo-reconnoiter
```

**Save the output** - you'll need:
- `AccessKeyId` → GitHub secret `AWS_ACCESS_KEY_ID`
- `SecretAccessKey` → GitHub secret `AWS_SECRET_ACCESS_KEY`

## Step 2: Attach IAM Policy

Create a policy with minimal required permissions:

```bash
# Create policy file
cat > /tmp/github-actions-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECSAccess",
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices"
      ],
      "Resource": [
        "arn:aws:ecs:us-east-1:*:service/repo-reconnoiter-prod/*"
      ]
    }
  ]
}
EOF

# Create policy
aws iam create-policy \
  --policy-name GitHubActionsRepoReconnoiter \
  --policy-document file:///tmp/github-actions-policy.json

# Attach policy to user (replace ACCOUNT_ID with your AWS account ID)
aws iam attach-user-policy \
  --user-name github-actions-repo-reconnoiter \
  --policy-arn arn:aws:iam::ACCOUNT_ID:policy/GitHubActionsRepoReconnoiter
```

## Step 3: Add GitHub Secrets

Go to your GitHub repository:
1. Navigate to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add the following secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `AWS_ACCESS_KEY_ID` | From Step 1 output | IAM user access key |
| `AWS_SECRET_ACCESS_KEY` | From Step 1 output | IAM user secret key |
| `SENTRY_AUTH_TOKEN` | Your Sentry auth token | Optional - for source uploads |

**Note**: `SENTRY_AUTH_TOKEN` is optional. If not set, Sentry source uploads will be skipped (see Dockerfile).

## Step 4: Test the Workflow

```bash
# Make a small change and push to main
git add .
git commit -m "Set up GitHub Actions CI/CD"
git push origin main

# Watch the workflow run in GitHub Actions tab
# URL: https://github.com/YOUR_USERNAME/repo-reconnoiter-kotlin/actions
```

## Workflow Behavior

**On every push to `main` branch:**
1. ✅ Run tests (`./gradlew test`)
2. ✅ Build Docker image (native AMD64, faster than M-series Mac!)
3. ✅ Tag with git commit SHA + `:latest`
4. ✅ Push to ECR
5. ✅ Force ECS deployment
6. ✅ Wait for service to stabilize
7. ✅ Report deployment success

**Deployment time:** ~5-7 minutes
- Build: ~3 minutes (native AMD64, no emulation)
- Push: ~1 minute
- ECS rolling update: ~2-3 minutes

## Benefits of CI/CD

✅ **Automatic deployments** - Push to main = automatic production deployment
✅ **Faster builds** - GitHub Actions runs on AMD64 natively (no QEMU emulation)
✅ **Test before deploy** - Tests must pass before deployment
✅ **Rollback capability** - Every commit tagged with SHA in ECR
✅ **Deployment visibility** - See status in GitHub Actions UI

## Rollback to Previous Version

If you need to rollback:

```bash
# List recent images with SHA tags
aws ecr describe-images \
  --repository-name repo-reconnoiter-prod \
  --query 'sort_by(imageDetails,& imagePushedAt)[-5:]' \
  --output table

# Deploy specific SHA
aws ecs update-service \
  --cluster repo-reconnoiter-prod \
  --service repo-reconnoiter-prod \
  --task-definition repo-reconnoiter-prod \
  --force-new-deployment

# Or manually tag old SHA as :latest and force deployment
docker tag <ECR_URI>:<OLD_SHA> <ECR_URI>:latest
docker push <ECR_URI>:latest
```

## Monitoring Deployments

**GitHub Actions UI:**
- View workflow runs: `https://github.com/YOUR_USERNAME/repo-reconnoiter-kotlin/actions`
- See logs, duration, and status

**AWS Console:**
- ECS Service: CloudFormation → RepoReconnoiter-API-prod → Resources
- CloudWatch Logs: `/ecs/repo-reconnoiter-prod`

**API Health:**
```bash
curl http://repo-reconnoiter-prod-1421305048.us-east-1.elb.amazonaws.com/api/v1/actuator/health
```

## Troubleshooting

**Build fails with "permission denied":**
- Check IAM policy is attached correctly
- Verify AWS credentials in GitHub secrets

**ECS deployment times out:**
- Check CloudWatch logs: `aws logs tail /ecs/repo-reconnoiter-prod --since 10m`
- Verify health check path is correct (`/api/v1/actuator/health`)

**Tests fail:**
- Fix tests locally first: `./gradlew test`
- Push fix to main to re-trigger deployment

## Security Notes

- ✅ IAM user has **minimal required permissions** (principle of least privilege)
- ✅ Secrets stored in GitHub Secrets (encrypted at rest)
- ✅ AWS credentials never logged in workflow output
- ✅ Only `main` branch triggers deployments
- ❌ Never commit AWS credentials to git

## Next Steps

Consider adding:
- [ ] Slack/Discord deployment notifications
- [ ] Staging environment (deploy feature branches)
- [ ] Manual approval step for production
- [ ] Automated integration tests
- [ ] Performance testing
