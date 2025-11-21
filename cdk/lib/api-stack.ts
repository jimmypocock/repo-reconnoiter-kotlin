import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cwactions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import { Construct } from 'constructs';

interface ApiStackProps extends cdk.StackProps {
  environmentName: string;
  vpc: ec2.IVpc;
  albSecurityGroup: ec2.ISecurityGroup;
  ecsSecurityGroup: ec2.ISecurityGroup;
  database: rds.IDatabaseInstance;
  dbSecret: secretsmanager.ISecret;
}

export class ApiStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: ApiStackProps) {
    super(scope, id, props);

    const { environmentName, vpc, albSecurityGroup, ecsSecurityGroup, database, dbSecret } = props;

    // ============================================
    // Application Secrets
    // ============================================

    // JWT secret
    const jwtSecret = new secretsmanager.Secret(this, 'JWTSecret', {
      secretName: `repo-reconnoiter-${environmentName}-jwt-secret`,
      description: 'JWT secret key for RepoReconnoiter',
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete with stack
      generateSecretString: {
        secretStringTemplate: JSON.stringify({}),
        generateStringKey: 'secret',
        excludePunctuation: true,
        includeSpace: false,
        passwordLength: 64,
      },
    });

    // GitHub OAuth credentials (manually update via AWS Console)
    const githubSecret = new secretsmanager.Secret(this, 'GitHubOAuthSecret', {
      secretName: `repo-reconnoiter-${environmentName}-github-oauth`,
      description: 'GitHub OAuth credentials for RepoReconnoiter',
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete with stack
      secretObjectValue: {
        clientId: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
        clientSecret: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
      },
    });

    // OpenAI API Key (manually update via AWS Console)
    const openaiSecret = new secretsmanager.Secret(this, 'OpenAISecret', {
      secretName: `repo-reconnoiter-${environmentName}-openai-key`,
      description: 'OpenAI API key for RepoReconnoiter',
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete with stack
      secretObjectValue: {
        apiKey: cdk.SecretValue.unsafePlainText('REPLACE_ME'),
      },
    });

    // ============================================
    // ECR Repository
    // ============================================
    const ecrRepository = new ecr.Repository(this, 'ECRRepository', {
      repositoryName: `repo-reconnoiter-${environmentName}`,
      imageScanOnPush: true,
      imageTagMutability: ecr.TagMutability.MUTABLE,
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete with stack (change to RETAIN for prod)
      emptyOnDelete: true, // Auto-delete images before deleting repository
      lifecycleRules: [
        {
          description: 'Keep last 10 images',
          maxImageCount: 10,
        },
      ],
    });

    // ============================================
    // Security Groups
    // ============================================
    // Note: All security groups are created in SecurityGroups stack and passed as props
    // This includes: ALB security group, ECS security group, and DB security group
    // All ingress/egress rules are also defined in SecurityGroups stack

    // ============================================
    // CloudWatch Logs
    // ============================================
    const logGroup = new logs.LogGroup(this, 'ECSLogGroup', {
      logGroupName: `/ecs/repo-reconnoiter-${environmentName}`,
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // ============================================
    // ECS Fargate Cluster
    // ============================================
    const cluster = new ecs.Cluster(this, 'ECSCluster', {
      clusterName: `repo-reconnoiter-${environmentName}`,
      vpc,
      containerInsightsV2: ecs.ContainerInsights.ENHANCED, // CloudWatch Container Insights with enhanced observability
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

    // Grant permissions to access secrets
    dbSecret.grantRead(taskExecutionRole);
    jwtSecret.grantRead(taskExecutionRole);
    githubSecret.grantRead(taskExecutionRole);
    openaiSecret.grantRead(taskExecutionRole);

    // Grant permissions to pull from ECR
    ecrRepository.grantPull(taskExecutionRole);

    // Task Role - Used by the running application
    const taskRole = new iam.Role(this, 'TaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      description: 'ECS Task Role for RepoReconnoiter application',
    });

    // Grant CloudWatch Logs permissions
    taskRole.addToPolicy(new iam.PolicyStatement({
      actions: [
        'logs:CreateLogStream',
        'logs:PutLogEvents',
        'logs:DescribeLogStreams',
        'logs:DescribeLogGroups',
      ],
      resources: ['*'],
    }));

    // Grant SSM Session Manager permissions (for debugging)
    taskRole.addToPolicy(new iam.PolicyStatement({
      actions: [
        'ssmmessages:CreateControlChannel',
        'ssmmessages:CreateDataChannel',
        'ssmmessages:OpenControlChannel',
        'ssmmessages:OpenDataChannel',
      ],
      resources: ['*'],
    }));

    // ============================================
    // ECS Task Definition
    // ============================================
    const taskDefinition = new ecs.FargateTaskDefinition(this, 'TaskDefinitionFinal', {
      family: `repo-reconnoiter-${environmentName}`,
      cpu: 1024, // 1 vCPU
      memoryLimitMiB: 2048, // 2 GB
      executionRole: taskExecutionRole,
      taskRole: taskRole,
    });

    const container = taskDefinition.addContainer('api', {
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
        APP_FRONTEND_URL: 'https://reporeconnoiter.com', // Frontend URL for CORS and OAuth redirects
        SENTRY_DSN: 'https://placeholder@sentry.io/placeholder', // Update with real Sentry DSN later
      },
      secrets: {
        DATABASE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
        JWT_SECRET: ecs.Secret.fromSecretsManager(jwtSecret, 'secret'),
        GITHUB_CLIENT_ID: ecs.Secret.fromSecretsManager(githubSecret, 'clientId'),
        GITHUB_CLIENT_SECRET: ecs.Secret.fromSecretsManager(githubSecret, 'clientSecret'),
        OPENAI_ACCESS_TOKEN: ecs.Secret.fromSecretsManager(openaiSecret, 'apiKey'),
      },
      healthCheck: {
        command: ['CMD-SHELL', 'curl -f http://localhost:8080/api/v1/actuator/health || exit 1'],
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
    const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
      loadBalancerName: `repo-reconnoiter-${environmentName}`,
      vpc,
      internetFacing: true,
      securityGroup: albSecurityGroup,
    });

    // Target Group
    const targetGroup = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
      targetGroupName: `repo-reconnoiter-${environmentName}`,
      vpc,
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targetType: elbv2.TargetType.IP,
      healthCheck: {
        path: '/api/v1/actuator/health',
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
        healthyHttpCodes: '200',
      },
      deregistrationDelay: cdk.Duration.seconds(30),
    });

    // HTTPS Listener (port 443) - optional, requires ACM certificate
    const certificateArn = process.env.ACM_CERTIFICATE_ARN;
    if (certificateArn) {
      const certificate = certificatemanager.Certificate.fromCertificateArn(
        this,
        'Certificate',
        certificateArn
      );

      const httpsListener = alb.addListener('HTTPSListener', {
        port: 443,
        protocol: elbv2.ApplicationProtocol.HTTPS,
        certificates: [certificate],
        open: true,
      });

      // Forward HTTPS traffic to target group
      httpsListener.addTargetGroups('ECS-HTTPS', {
        targetGroups: [targetGroup],
      });

      // HTTP Listener (port 80) - redirect to HTTPS
      alb.addListener('HTTPListener', {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        open: true,
        defaultAction: elbv2.ListenerAction.redirect({
          protocol: 'HTTPS',
          port: '443',
          permanent: true,
        }),
      });
    } else {
      // HTTP Listener (port 80) - direct traffic (no HTTPS available)
      const httpListener = alb.addListener('HTTPListener', {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        open: true,
      });

      // Forward HTTP traffic to target group
      httpListener.addTargetGroups('ECS', {
        targetGroups: [targetGroup],
      });
    }

    // ============================================
    // ECS Fargate Service
    // ============================================
    // desiredCount can be overridden via context: cdk deploy --context desiredCount=0
    // Defaults to 2 for production-ready deployments
    const desiredCount = this.node.tryGetContext('desiredCount') ?? 2;

    const service = new ecs.FargateService(this, 'ECSService', {
      serviceName: `repo-reconnoiter-${environmentName}`,
      cluster,
      taskDefinition,
      desiredCount: desiredCount, // Production-ready default: 2 tasks for high availability
      assignPublicIp: false, // Tasks in private subnet
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      },
      securityGroups: [ecsSecurityGroup],
      healthCheckGracePeriod: cdk.Duration.seconds(60),
      enableExecuteCommand: true, // Enable ECS Exec for debugging
      minHealthyPercent: 100, // Keep all tasks running during deployment
      maxHealthyPercent: 200, // Allow double capacity during deployment
    });

    // Attach service to ALB target group
    service.attachToApplicationTargetGroup(targetGroup);

    // Auto-scaling based on CPU and memory
    const scaling = service.autoScaleTaskCount({
      minCapacity: 2,
      maxCapacity: 5,
    });

    scaling.scaleOnCpuUtilization('CPUScaling', {
      targetUtilizationPercent: 70,
      scaleInCooldown: cdk.Duration.seconds(60),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });

    scaling.scaleOnMemoryUtilization('MemoryScaling', {
      targetUtilizationPercent: 70,
      scaleInCooldown: cdk.Duration.seconds(60),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });

    // ============================================
    // CloudWatch Alarms
    // ============================================

    // SNS Topic for alarms
    const alarmTopic = new sns.Topic(this, 'AlarmTopic', {
      topicName: `repo-reconnoiter-${environmentName}-alarms`,
      displayName: 'RepoReconnoiter Production Alarms',
    });

    // Subscribe email to alarm topic
    alarmTopic.addSubscription(
      new subscriptions.EmailSubscription('jimmycpocock+RepoReconnoiter@gmail.com')
    );

    // Alarm: Zero running tasks (critical outage)
    const zeroTasksAlarm = new cloudwatch.Alarm(this, 'ZeroTasksAlarm', {
      alarmName: `${environmentName}-zero-ecs-tasks`,
      metric: new cloudwatch.Metric({
        namespace: 'AWS/ECS',
        metricName: 'RunningTaskCount',
        dimensionsMap: {
          ServiceName: service.serviceName,
          ClusterName: cluster.clusterName,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(1),
      }),
      threshold: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      alarmDescription: '[CRITICAL] All ECS tasks are down! Application is unavailable.',
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });
    zeroTasksAlarm.addAlarmAction(new cwactions.SnsAction(alarmTopic));

    // ============================================
    // CloudFormation Outputs
    // ============================================
    new cdk.CfnOutput(this, 'ECRRepositoryUri', {
      value: ecrRepository.repositoryUri,
      description: 'ECR Repository URI - Push your Docker image here',
      exportName: `RepoReconnoiter-API-${environmentName}-ecr-uri`,
    });

    new cdk.CfnOutput(this, 'LoadBalancerURL', {
      value: `http://${alb.loadBalancerDnsName}`,
      description: 'Application Load Balancer URL (use this to access the API)',
      exportName: `RepoReconnoiter-API-${environmentName}-alb-url`,
    });

    new cdk.CfnOutput(this, 'ECSClusterName', {
      value: cluster.clusterName,
      description: 'ECS Cluster name',
      exportName: `RepoReconnoiter-API-${environmentName}-ecs-cluster`,
    });

    new cdk.CfnOutput(this, 'ECSServiceName', {
      value: service.serviceName,
      description: 'ECS Service name',
      exportName: `RepoReconnoiter-API-${environmentName}-ecs-service`,
    });

    new cdk.CfnOutput(this, 'CloudWatchLogGroup', {
      value: logGroup.logGroupName,
      description: 'CloudWatch Log Group for ECS tasks',
      exportName: `RepoReconnoiter-API-${environmentName}-log-group`,
    });
  }
}
