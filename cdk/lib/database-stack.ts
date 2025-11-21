import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cwactions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

interface DatabaseStackProps extends cdk.StackProps {
  environmentName: string;
  vpc: ec2.IVpc;
  dbSecurityGroup: ec2.ISecurityGroup;
}

export class DatabaseStack extends cdk.Stack {
  public readonly database: rds.DatabaseInstance;
  public readonly dbSecret: secretsmanager.Secret;

  constructor(scope: Construct, id: string, props: DatabaseStackProps) {
    super(scope, id, props);

    const { environmentName, vpc, dbSecurityGroup } = props;

    // ============================================
    // Database Credentials Secret
    // ============================================
    this.dbSecret = new secretsmanager.Secret(this, 'DatabaseSecret', {
      secretName: `repo-reconnoiter-${environmentName}-db-credentials`,
      description: 'Database credentials for RepoReconnoiter',
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete with stack
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'reconnoiter' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        passwordLength: 32,
      },
    });

    // ============================================
    // RDS MySQL Database
    // ============================================
    this.database = new rds.DatabaseInstance(this, 'Database', {
      instanceIdentifier: `repo-reconnoiter-${environmentName}-db`,
      engine: rds.DatabaseInstanceEngine.mysql({
        version: rds.MysqlEngineVersion.VER_8_0,
      }),
      instanceType: ec2.InstanceType.of(
        ec2.InstanceClass.T3,
        ec2.InstanceSize.MICRO
      ),
      vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED, // Database subnets
      },
      securityGroups: [dbSecurityGroup],
      credentials: rds.Credentials.fromSecret(this.dbSecret),
      databaseName: 'reconnoiter',
      allocatedStorage: 20,
      maxAllocatedStorage: 100, // Auto-scaling storage
      storageType: rds.StorageType.GP3,
      storageEncrypted: true,
      multiAz: false, // Enable for production HA
      backupRetention: cdk.Duration.days(7),
      deleteAutomatedBackups: true, // Clean up everything on deletion (change to false for prod)
      deletionProtection: false, // Allow clean deletion during setup (change to true for prod)
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Delete DB with stack (change to SNAPSHOT for prod)
      publiclyAccessible: false,
      enablePerformanceInsights: false,
      cloudwatchLogsExports: [ 'error', 'general', 'slowquery' ], // Export MySQL logs to CloudWatch
    });

    // ============================================
    // CloudWatch Alarms
    // ============================================

    // SNS Topic for database alarms
    const alarmTopic = new sns.Topic(this, 'DatabaseAlarmTopic', {
      topicName: `repo-reconnoiter-${environmentName}-db-alarms`,
      displayName: 'RepoReconnoiter Database Alarms',
    });

    // Subscribe email to alarm topic
    alarmTopic.addSubscription(
      new subscriptions.EmailSubscription('jimmycpocock+RepoReconnoiter@gmail.com')
    );

    // WARNING: Alarm when RDS is running out of disk space
    const lowStorageAlarm = new cloudwatch.Alarm(this, 'LowStorageAlarm', {
      alarmName: `${environmentName}-rds-low-storage`,
      metric: this.database.metricFreeStorageSpace({
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 2 * 1024 * 1024 * 1024, // 2 GB in bytes
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 1,
      alarmDescription: '[WARNING] RDS has less than 2GB storage remaining. Consider increasing storage.',
    });

    lowStorageAlarm.addAlarmAction(new cwactions.SnsAction(alarmTopic));

    // ============================================
    // CloudFormation Outputs
    // ============================================
    new cdk.CfnOutput(this, 'DatabaseEndpoint', {
      value: this.database.dbInstanceEndpointAddress,
      description: 'RDS MySQL endpoint',
      exportName: `RepoReconnoiter-Database-${environmentName}-endpoint`,
    });

    new cdk.CfnOutput(this, 'DatabasePort', {
      value: this.database.dbInstanceEndpointPort,
      description: 'RDS MySQL port',
      exportName: `RepoReconnoiter-Database-${environmentName}-port`,
    });

    new cdk.CfnOutput(this, 'DatabaseSecretArn', {
      value: this.dbSecret.secretArn,
      description: 'ARN of database credentials secret',
      exportName: `RepoReconnoiter-Database-${environmentName}-secret-arn`,
    });
  }
}
