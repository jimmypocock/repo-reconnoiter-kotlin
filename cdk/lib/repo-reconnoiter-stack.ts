import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cwactions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

export interface RepoReconnoiterStackProps extends cdk.StackProps {
  /**
   * Environment (dev, staging, prod)
   */
  environmentName: string;

  /**
   * Custom domain name for the API (optional)
   * Example: api.reporeconnoiter.com
   */
  customDomainName?: string;
}

export class RepoReconnoiterStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: RepoReconnoiterStackProps) {
    super(scope, id, props);

    const { environmentName } = props;

    // ============================================
    // VPC for RDS and ECS
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
    // Security Groups for ECS
    // ============================================

    const ecsSecurityGroup = new ec2.SecurityGroup(this, 'ECSSecurityGroup', {
      vpc,
      securityGroupName: `repo-reconnoiter-${environmentName}-ecs-sg`,
      description: 'Security group for ECS tasks',
      allowAllOutbound: true,
    });

    // Allow ECS tasks to access RDS
    dbSecurityGroup.addIngressRule(
      ecsSecurityGroup,
      ec2.Port.tcp(3306),
      'Allow ECS tasks to access RDS MySQL'
    );

    // ============================================
    // CloudWatch Logs
    // ============================================
    const logGroup = new logs.LogGroup(this, 'ECSLogGroup', {
      logGroupName: `/ecs/repo-reconnoiter-${environmentName}`,
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: environmentName === 'prod'
        ? cdk.RemovalPolicy.RETAIN
        : cdk.RemovalPolicy.DESTROY,
    });

    // ============================================
    // ECS Fargate Cluster
    // ============================================
    const cluster = new ecs.Cluster(this, 'ECSCluster', {
      clusterName: `repo-reconnoiter-${environmentName}`,
      vpc,
      containerInsights: true, // CloudWatch Container Insights for monitoring
    });

    // ============================================
    // IAM Roles for ECS
    // ============================================

    // Task Execution Role - Used by ECS to pull images and access secrets
    const taskExecutionRole = new iam.Role(this, 'TaskExecutionRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      description: 'ECS Task Execution Role for pulling images and accessing secrets',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
      ],
    });

    // Grant access to secrets
    dbSecret.grantRead(taskExecutionRole);
    jwtSecret.grantRead(taskExecutionRole);
    githubSecret.grantRead(taskExecutionRole);
    openaiSecret.grantRead(taskExecutionRole);

    // Grant access to ECR
    ecrRepository.grantPull(taskExecutionRole);

    // Task Role - Used by the application itself
    const taskRole = new iam.Role(this, 'TaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      description: 'ECS Task Role for application runtime permissions',
    });

    // Application doesn't need any special permissions yet
    // Add policies here if the app needs to access S3, SQS, etc.

    // ============================================
    // ECS Task Definition
    // ============================================
    const taskDefinition = new ecs.FargateTaskDefinition(this, 'TaskDefinition', {
      family: `repo-reconnoiter-${environmentName}`,
      cpu: 256, // 0.25 vCPU
      memoryLimitMiB: 512, // 0.5 GB
      executionRole: taskExecutionRole,
      taskRole: taskRole,
    });

    // Add container to task definition
    const container = taskDefinition.addContainer('app', {
      containerName: 'repo-reconnoiter-api',
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'ecs',
        logGroup: logGroup,
      }),
      environment: {
        SPRING_PROFILES_ACTIVE: 'prod',
        SERVER_PORT: '8080',
        DATABASE_URL: `jdbc:mysql://${database.dbInstanceEndpointAddress}:3306/reconnoiter`,
        DATABASE_USERNAME: 'reconnoiter',
      },
      secrets: {
        DATABASE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
        JWT_SECRET: ecs.Secret.fromSecretsManager(jwtSecret, 'secret'),
        GITHUB_CLIENT_ID: ecs.Secret.fromSecretsManager(githubSecret, 'clientId'),
        GITHUB_CLIENT_SECRET: ecs.Secret.fromSecretsManager(githubSecret, 'clientSecret'),
        OPENAI_ACCESS_TOKEN: ecs.Secret.fromSecretsManager(openaiSecret, 'apiKey'),
      },
      healthCheck: {
        command: ['CMD-SHELL', 'curl -f http://localhost:8080/actuator/health || exit 1'],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(60),
      },
    });

    // Map container port
    container.addPortMappings({
      containerPort: 8080,
      protocol: ecs.Protocol.TCP,
    });

    // ============================================
    // Application Load Balancer
    // ============================================

    // Security group for ALB
    const albSecurityGroup = new ec2.SecurityGroup(this, 'ALBSecurityGroup', {
      vpc,
      securityGroupName: `repo-reconnoiter-${environmentName}-alb-sg`,
      description: 'Security group for Application Load Balancer',
      allowAllOutbound: true,
    });

    // Allow HTTPS traffic from internet
    albSecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(443),
      'Allow HTTPS from internet'
    );

    // Allow HTTP traffic from internet (for health checks and redirect to HTTPS)
    albSecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(80),
      'Allow HTTP from internet'
    );

    // Allow ALB to reach ECS tasks
    ecsSecurityGroup.addIngressRule(
      albSecurityGroup,
      ec2.Port.tcp(8080),
      'Allow ALB to reach ECS tasks'
    );

    // Create ALB
    const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
      loadBalancerName: `repo-reconnoiter-${environmentName}`,
      vpc,
      internetFacing: true,
      securityGroup: albSecurityGroup,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC,
      },
    });

    // Create target group for ECS service
    const targetGroup = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
      targetGroupName: `repo-reconnoiter-${environmentName}`,
      vpc,
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targetType: elbv2.TargetType.IP,
      healthCheck: {
        path: '/actuator/health',
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
        healthyHttpCodes: '200',
      },
      deregistrationDelay: cdk.Duration.seconds(30),
    });

    // Add HTTP listener (will redirect to HTTPS in production)
    const httpListener = alb.addListener('HTTPListener', {
      port: 80,
      protocol: elbv2.ApplicationProtocol.HTTP,
      defaultAction: elbv2.ListenerAction.forward([targetGroup]),
    });

    // TODO: Add HTTPS listener with ACM certificate for production
    // const httpsListener = alb.addListener('HTTPSListener', {
    //   port: 443,
    //   protocol: elbv2.ApplicationProtocol.HTTPS,
    //   certificates: [certificate],
    //   defaultAction: elbv2.ListenerAction.forward([targetGroup]),
    // });

    // ============================================
    // ECS Service
    // ============================================
    const service = new ecs.FargateService(this, 'ECSService', {
      serviceName: `repo-reconnoiter-${environmentName}`,
      cluster,
      taskDefinition,
      desiredCount: environmentName === 'prod' ? 2 : 1,
      assignPublicIp: false, // Tasks in private subnet
      securityGroups: [ecsSecurityGroup],
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      },
      healthCheckGracePeriod: cdk.Duration.seconds(60),
      enableExecuteCommand: true, // Allow SSH into containers for debugging
    });

    // Attach service to target group
    service.attachToApplicationTargetGroup(targetGroup);

    // Auto-scaling configuration
    const scaling = service.autoScaleTaskCount({
      minCapacity: environmentName === 'prod' ? 2 : 1,
      maxCapacity: environmentName === 'prod' ? 5 : 3,
    });

    // Scale based on CPU utilization
    scaling.scaleOnCpuUtilization('CPUScaling', {
      targetUtilizationPercent: 70,
      scaleInCooldown: cdk.Duration.seconds(60),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });

    // Scale based on memory utilization
    scaling.scaleOnMemoryUtilization('MemoryScaling', {
      targetUtilizationPercent: 70,
      scaleInCooldown: cdk.Duration.seconds(60),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });

    // ============================================
    // CloudWatch Alarms
    // ============================================

    // SNS Topic for alarm notifications
    const alarmTopic = new sns.Topic(this, 'AlarmTopic', {
      topicName: `repo-reconnoiter-${environmentName}-alarms`,
      displayName: 'RepoReconnoiter Infrastructure Alerts',
    });

    // Subscribe email to SNS topic (you'll need to confirm subscription via email)
    alarmTopic.addSubscription(
      new subscriptions.EmailSubscription('jimmycpocock+RepoReconnoiter@gmail.com')
    );

    // CRITICAL: Alarm when all ECS tasks are down (app completely unavailable)
    const zeroTasksAlarm = new cloudwatch.Alarm(this, 'ZeroTasksAlarm', {
      alarmName: `${environmentName}-zero-ecs-tasks`,
      metric: service.metricRunningTaskCount({
        statistic: 'Average',
        period: cdk.Duration.minutes(1),
      }),
      threshold: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      alarmDescription: '🚨 CRITICAL: All ECS tasks are down! Application is unavailable.',
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    zeroTasksAlarm.addAlarmAction(new cwactions.SnsAction(alarmTopic));

    // WARNING: Alarm when RDS is running out of disk space
    const lowStorageAlarm = new cloudwatch.Alarm(this, 'LowStorageAlarm', {
      alarmName: `${environmentName}-rds-low-storage`,
      metric: database.metricFreeStorageSpace({
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 2 * 1024 * 1024 * 1024, // 2 GB in bytes
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 1,
      alarmDescription: '⚠️ WARNING: RDS has less than 2GB storage remaining. Consider increasing storage.',
    });

    lowStorageAlarm.addAlarmAction(new cwactions.SnsAction(alarmTopic));

    // ============================================
    // Outputs
    // ============================================

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
      description: 'RDS MySQL endpoint',
      exportName: `repo-reconnoiter-${environmentName}-db-endpoint`,
    });

    new cdk.CfnOutput(this, 'DatabasePort', {
      value: database.dbInstanceEndpointPort,
      description: 'RDS MySQL port',
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

    new cdk.CfnOutput(this, 'LoadBalancerURL', {
      value: `http://${alb.loadBalancerDnsName}`,
      description: 'Application Load Balancer URL (use this to access the API)',
      exportName: `repo-reconnoiter-${environmentName}-alb-url`,
    });

    new cdk.CfnOutput(this, 'ECSClusterName', {
      value: cluster.clusterName,
      description: 'ECS Cluster name',
      exportName: `repo-reconnoiter-${environmentName}-ecs-cluster`,
    });

    new cdk.CfnOutput(this, 'ECSServiceName', {
      value: service.serviceName,
      description: 'ECS Service name',
      exportName: `repo-reconnoiter-${environmentName}-ecs-service`,
    });

    new cdk.CfnOutput(this, 'CloudWatchLogGroup', {
      value: logGroup.logGroupName,
      description: 'CloudWatch Log Group for ECS tasks',
      exportName: `repo-reconnoiter-${environmentName}-log-group`,
    });

    new cdk.CfnOutput(this, 'AlarmTopicArn', {
      value: alarmTopic.topicArn,
      description: 'SNS Topic ARN for CloudWatch alarms - Subscribe your email here',
      exportName: `repo-reconnoiter-${environmentName}-alarm-topic-arn`,
    });
  }
}
