import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as apprunner from 'aws-cdk-lib/aws-apprunner';
import * as apigatewayv2 from 'aws-cdk-lib/aws-apigatewayv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

export interface RepoReconnoiterStackProps extends cdk.StackProps {
  /**
   * Environment (dev, staging, prod)
   */
  environmentName: string;

  /**
   * GitHub repository URL for auto-deployment
   */
  githubRepositoryUrl?: string;

  /**
   * GitHub branch to track (default: main)
   */
  githubBranch?: string;

  /**
   * Custom domain name for API Gateway (optional)
   */
  customDomainName?: string;
}

export class RepoReconnoiterStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: RepoReconnoiterStackProps) {
    super(scope, id, props);

    const { environmentName } = props;

    // ============================================
    // VPC for RDS and App Runner
    // ============================================
    const vpc = new ec2.Vpc(this, 'RepoReconnoiterVPC', {
      vpcName: `repo-reconnoiter-${environmentName}-vpc`,
      maxAzs: 2, // Multi-AZ for high availability
      natGateways: 1, // One NAT Gateway (reduce cost)
      subnetConfiguration: [
        {
          name: 'Public',
          subnetType: ec2.SubnetType.PUBLIC,
          cidrMask: 24,
        },
        {
          name: 'Private',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          cidrMask: 24,
        },
        {
          name: 'Database',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
          cidrMask: 24,
        },
      ],
    });

    // ============================================
    // Secrets Manager - Store sensitive configuration
    // ============================================

    // Database credentials
    const dbSecret = new secretsmanager.Secret(this, 'DatabaseSecret', {
      secretName: `repo-reconnoiter-${environmentName}-db-credentials`,
      description: 'Database credentials for RepoReconnoiter',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'reconnoiter' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        includeSpace: false,
        passwordLength: 32,
      },
    });

    // JWT secret
    const jwtSecret = new secretsmanager.Secret(this, 'JWTSecret', {
      secretName: `repo-reconnoiter-${environmentName}-jwt-secret`,
      description: 'JWT secret key for RepoReconnoiter',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({}),
        generateStringKey: 'secret',
        excludePunctuation: true,
        includeSpace: false,
        passwordLength: 64,
      },
    });

    // GitHub OAuth credentials (manually set via AWS Console)
    const githubSecret = new secretsmanager.Secret(this, 'GitHubOAuthSecret', {
      secretName: `repo-reconnoiter-${environmentName}-github-oauth`,
      description: 'GitHub OAuth credentials for RepoReconnoiter',
      secretObjectValue: {
        clientId: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
        clientSecret: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
      },
    });

    // OpenAI API key (manually set via AWS Console)
    const openaiSecret = new secretsmanager.Secret(this, 'OpenAISecret', {
      secretName: `repo-reconnoiter-${environmentName}-openai-key`,
      description: 'OpenAI API key for RepoReconnoiter',
      secretObjectValue: {
        apiKey: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
      },
    });

    // ============================================
    // RDS MySQL Database
    // ============================================
    const dbSecurityGroup = new ec2.SecurityGroup(this, 'DatabaseSecurityGroup', {
      vpc,
      securityGroupName: `repo-reconnoiter-${environmentName}-db-sg`,
      description: 'Security group for RepoReconnoiter RDS database',
      allowAllOutbound: true,
    });

    const database = new rds.DatabaseInstance(this, 'Database', {
      instanceIdentifier: `repo-reconnoiter-${environmentName}-db`,
      engine: rds.DatabaseInstanceEngine.mysql({
        version: rds.MysqlEngineVersion.VER_8_0,
      }),
      instanceType: ec2.InstanceType.of(
        ec2.InstanceClass.T3,
        ec2.InstanceSize.MICRO
      ),
      credentials: rds.Credentials.fromSecret(dbSecret),
      vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
      },
      securityGroups: [dbSecurityGroup],
      allocatedStorage: 20,
      maxAllocatedStorage: 100, // Auto-scaling storage
      storageType: rds.StorageType.GP3,
      storageEncrypted: true,
      backupRetention: cdk.Duration.days(7),
      deleteAutomatedBackups: environmentName !== 'prod',
      removalPolicy: environmentName === 'prod'
        ? cdk.RemovalPolicy.RETAIN
        : cdk.RemovalPolicy.DESTROY,
      deletionProtection: environmentName === 'prod',
      publiclyAccessible: false,
      multiAz: environmentName === 'prod', // Only prod uses Multi-AZ
      enablePerformanceInsights: false, // Enable if needed
      cloudwatchLogsExports: ['error', 'general', 'slowquery'], // Export MySQL logs to CloudWatch
      databaseName: 'reconnoiter',
    });

    // ============================================
    // ECR Repository for Docker images
    // ============================================
    const ecrRepository = new ecr.Repository(this, 'ECRRepository', {
      repositoryName: `repo-reconnoiter-${environmentName}`,
      imageScanOnPush: true,
      imageTagMutability: ecr.TagMutability.MUTABLE,
      removalPolicy: environmentName === 'prod'
        ? cdk.RemovalPolicy.RETAIN
        : cdk.RemovalPolicy.DESTROY,
      lifecycleRules: [
        {
          description: 'Keep last 10 images',
          maxImageCount: 10,
        },
      ],
    });

    // ============================================
    // App Runner Service
    // ============================================

    // IAM role for App Runner instance
    const instanceRole = new iam.Role(this, 'AppRunnerInstanceRole', {
      assumedBy: new iam.ServicePrincipal('tasks.apprunner.amazonaws.com'),
      description: 'Instance role for App Runner service',
    });

    // Grant access to secrets
    dbSecret.grantRead(instanceRole);
    jwtSecret.grantRead(instanceRole);
    githubSecret.grantRead(instanceRole);
    openaiSecret.grantRead(instanceRole);

    // Grant VPC access (for RDS)
    instanceRole.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonVPCFullAccess')
    );

    // IAM role for App Runner access (ECR, etc.)
    const accessRole = new iam.Role(this, 'AppRunnerAccessRole', {
      assumedBy: new iam.ServicePrincipal('build.apprunner.amazonaws.com'),
      description: 'Access role for App Runner to pull from ECR',
    });

    ecrRepository.grantPull(accessRole);

    // VPC connector for App Runner to access RDS
    const vpcConnector = new apprunner.CfnVpcConnector(this, 'AppRunnerVPCConnector', {
      subnets: vpc.selectSubnets({
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      }).subnetIds,
      securityGroups: [dbSecurityGroup.securityGroupId],
    });

    // Note: App Runner service creation requires either source code or ECR image
    // For production, you'll push Docker image to ECR first, then create the service
    // This is a template - you'll need to fill in the imageIdentifier after first push

    new cdk.CfnOutput(this, 'ECRRepositoryUri', {
      value: ecrRepository.repositoryUri,
      description: 'ECR Repository URI - Push your Docker image here',
      exportName: `repo-reconnoiter-${environmentName}-ecr-uri`,
    });

    new cdk.CfnOutput(this, 'ECRPushCommands', {
      value: [
        `aws ecr get-login-password --region ${this.region} | docker login --username AWS --password-stdin ${ecrRepository.repositoryUri}`,
        `docker build -t repo-reconnoiter .`,
        `docker tag repo-reconnoiter:latest ${ecrRepository.repositoryUri}:latest`,
        `docker push ${ecrRepository.repositoryUri}:latest`,
      ].join('\n'),
      description: 'Commands to push Docker image to ECR',
    });

    // ============================================
    // Outputs
    // ============================================
    new cdk.CfnOutput(this, 'DatabaseEndpoint', {
      value: database.dbInstanceEndpointAddress,
      description: 'RDS PostgreSQL endpoint',
      exportName: `repo-reconnoiter-${environmentName}-db-endpoint`,
    });

    new cdk.CfnOutput(this, 'DatabasePort', {
      value: database.dbInstanceEndpointPort,
      description: 'RDS PostgreSQL port',
      exportName: `repo-reconnoiter-${environmentName}-db-port`,
    });

    new cdk.CfnOutput(this, 'DatabaseSecretArn', {
      value: dbSecret.secretArn,
      description: 'ARN of database credentials secret',
      exportName: `repo-reconnoiter-${environmentName}-db-secret-arn`,
    });

    new cdk.CfnOutput(this, 'JWTSecretArn', {
      value: jwtSecret.secretArn,
      description: 'ARN of JWT secret',
      exportName: `repo-reconnoiter-${environmentName}-jwt-secret-arn`,
    });

    new cdk.CfnOutput(this, 'GitHubSecretArn', {
      value: githubSecret.secretArn,
      description: 'ARN of GitHub OAuth secret',
      exportName: `repo-reconnoiter-${environmentName}-github-secret-arn`,
    });

    new cdk.CfnOutput(this, 'OpenAISecretArn', {
      value: openaiSecret.secretArn,
      description: 'ARN of OpenAI API key secret',
      exportName: `repo-reconnoiter-${environmentName}-openai-secret-arn`,
    });
  }
}
