#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { RepoReconnoiterStack } from '../lib/repo-reconnoiter-stack';

const app = new cdk.App();

// Environment from context or environment variables
const environmentName = app.node.tryGetContext('environment') || process.env.ENVIRONMENT || 'dev';
const awsAccount = process.env.CDK_DEFAULT_ACCOUNT;
const awsRegion = process.env.CDK_DEFAULT_REGION || 'us-east-1';

// Create stack
new RepoReconnoiterStack(app, `RepoReconnoiter-${environmentName}`, {
  environmentName,

  env: {
    account: awsAccount,
    region: awsRegion,
  },

  // Stack tags
  tags: {
    Application: 'RepoReconnoiter',
    Environment: environmentName,
    ManagedBy: 'CDK',
  },

  // Stack description
  description: `RepoReconnoiter API infrastructure (${environmentName})`,
});

app.synth();
