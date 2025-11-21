#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { NetworkStack } from '../lib/network-stack';
import { SecurityGroupsStack } from '../lib/security-groups-stack';
import { DatabaseStack } from '../lib/database-stack';
import { ApiStack } from '../lib/api-stack';
import * as dotenv from 'dotenv';

// Load environment variables from .env file
dotenv.config();

const app = new cdk.App();

// Get configuration from environment variables (required - no defaults)
const environmentName = app.node.tryGetContext('environment') || process.env.ENVIRONMENT;
const awsAccount = process.env.CDK_DEFAULT_ACCOUNT;
const awsRegion = process.env.AWS_REGION;

// Validate required environment variables
if (!environmentName) {
  throw new Error('ENVIRONMENT variable is required. Set it in cdk/.env (see .env.example)');
}
if (!awsRegion) {
  throw new Error('AWS_REGION variable is required. Set it in cdk/.env (see .env.example)');
}
if (!awsAccount) {
  throw new Error('AWS account not detected. Run: aws sso login --profile YOUR_PROFILE');
}

const env = {
  account: awsAccount,
  region: awsRegion,
};

const tags = {
  Application: 'RepoReconnoiter',
  Environment: environmentName,
  ManagedBy: 'CDK',
};

// ============================================
// Stack 1: Network (VPC, Subnets, NAT)
// ============================================
const networkStack = new NetworkStack(app, `RepoReconnoiter-Network-${environmentName}`, {
  environmentName,
  env,
  tags,
  description: `RepoReconnoiter Network infrastructure (${environmentName})`,
});

// ============================================
// Stack 2: SecurityGroups (All security groups and rules)
// ============================================
const securityGroupsStack = new SecurityGroupsStack(app, `RepoReconnoiter-SecurityGroups-${environmentName}`, {
  environmentName,
  vpc: networkStack.vpc,
  env,
  tags,
  description: `RepoReconnoiter Security Groups (${environmentName})`,
});
securityGroupsStack.addDependency(networkStack); // Deploy after network

// ============================================
// Stack 3: Database (RDS)
// ============================================
const databaseStack = new DatabaseStack(app, `RepoReconnoiter-Database-${environmentName}`, {
  environmentName,
  vpc: networkStack.vpc,
  dbSecurityGroup: securityGroupsStack.dbSecurityGroup,
  env,
  tags,
  description: `RepoReconnoiter Database infrastructure (${environmentName})`,
});
databaseStack.addDependency(networkStack); // Deploy after network
databaseStack.addDependency(securityGroupsStack); // Deploy after security groups

// ============================================
// Stack 4: API (ECS, ALB, ECR)
// ============================================
const apiStack = new ApiStack(app, `RepoReconnoiter-API-${environmentName}`, {
  environmentName,
  vpc: networkStack.vpc,
  albSecurityGroup: securityGroupsStack.albSecurityGroup,
  ecsSecurityGroup: securityGroupsStack.ecsSecurityGroup,
  database: databaseStack.database,
  dbSecret: databaseStack.dbSecret,
  env,
  tags,
  description: `RepoReconnoiter Application infrastructure (${environmentName})`,
});
apiStack.addDependency(networkStack); // Deploy after network
apiStack.addDependency(securityGroupsStack); // Deploy after security groups
apiStack.addDependency(databaseStack); // Deploy after database

app.synth();
