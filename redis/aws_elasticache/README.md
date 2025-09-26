# AWS ElastiCache Flavor

Deploy and manage Redis using AWS ElastiCache service. Defaults are optimized for quick setup with minimal cost, perfect for development and testing environments.

## Default Configuration Philosophy

This flavor uses **cost-optimized defaults** to help you get started quickly:
- **Single node** setup without replicas (add replicas for production)
- **No encryption** by default (enable for production/sensitive data)
- **No automatic backups** (configure retention for production)
- **Single AZ deployment** (enable Multi-AZ for production)

## Features

- **Quick Start**: Minimal required configuration - just provide subnet and security groups
- **Cost Optimized**: Defaults minimize AWS costs for development/testing
- **Production Ready**: All production features available when needed
- **Cluster Mode Support**: Optional Redis Cluster mode for horizontal scaling
- **Flexible Scaling**: Add replicas and enable HA features as needed

## AWS ElastiCache for Redis Flavour Configuration

### Properties

| Property                      | Type                                   | Required | Description                                                                                                                                                                                                                                                                               |
|-------------------------------|----------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cacheSubnetGroupName`        | string                                 | **Yes**  | The name of the ElastiCache Subnet Group, which defines the VPC and subnets for the cluster. This is fundamental for placing Redis within your private network.                                                                                                                           |
| `replicationGroupId`          | string                                 | **Yes**  | Unique identifier for the replication group. Must be 1-40 alphanumeric characters or hyphens, begin with a letter, and contain no spaces or uppercase letters. This becomes the AWS resource name. **Required by AWS API**.                                                               |
| `securityGroupIds`            | string[]                               | **Yes**  | A list of VPC Security Group IDs that act as a virtual firewall for the cluster. This is a critical security setting that controls network access to your Redis nodes.                                                                                                                    |
| `atRestEncryptionEnabled`     | boolean                                | No       | Enables encryption of data stored on disk (backups and swap files). Enable for production workloads with compliance requirements. **Default: `false`** (disabled to minimize cost and complexity for development/testing).                                                                |
| `autoMinorVersionUpgrade`     | boolean                                | No       | Automatically apply minor Redis version upgrades during maintenance windows for security patches and bug fixes. **Default: `true`** (recommended for security). Set `false` only with strict version control requirements.                                                                |
| `automaticFailoverEnabled`    | boolean                                | No       | If `true`, ElastiCache automatically promotes a replica if the primary fails. Requires at least one replica. Enable for production workloads. **Default: `false`** (disabled to minimize complexity and cost for development/testing).                                                    |
| `cacheNodeType`               | string                                 | No       | The AWS instance type for the Redis nodes (e.g., `cache.t3.micro`). This is the primary driver of performance and cost, and must be chosen carefully based on your workload and budget. **Default: `cache.t4g.micro`** (smallest ARM-based general-purpose instance for cost efficiency). |
| `ipDiscovery`                 | string                                 | No       | IP version for cluster discovery. Requires Redis 6.2+. **Default: `'ipv4'`**. Use `'ipv6'` only if your infrastructure requires it and clients support it. Possible values are: `ipv4`, `ipv6`.                                                                                           |
| `kmsKeyId`                    | string                                 | No       | ARN of customer-managed KMS key for at-rest encryption. Only applies when `atRestEncryptionEnabled: true`. **Production:** Use customer-managed keys for compliance (HIPAA, PCI-DSS). Ensure proper key rotation policies.                                                                |
| `logDeliveryConfigurations`   | [object](#logdeliveryconfigurations)[] | No       | Log shipping configuration for slow-log and engine-log. Each object must specify `logType` (slow-log or engine-log), `destinationType` (cloudwatch-logs or kinesis-firehose), and `destinationDetails`. **Production:** Enable slow-log to CloudWatch for performance debugging.          |
| `multiAzEnabled`              | boolean                                | No       | If `true`, spreads replicas across multiple Availability Zones for resilience against AZ failures. Enable for production workloads. **Default: `false`** (single AZ to minimize cost for development/testing).                                                                            |
| `notificationTopicArn`        | string                                 | No       | SNS topic ARN for ElastiCache events (failovers, node replacements, scaling, maintenance). **Production:** Essential for operational visibility. Create dedicated SNS topics per environment and integrate with your alerting system.                                                     |
| `numNodeGroups`               | number                                 | No       | The number of shards for a Redis Cluster. Only applicable if `clusterModeEnabled` is true. Valid range: 1-500. **Default: `1`** (single shard).                                                                                                                                           |
| `parameterGroupName`          | string                                 | No       | The name of the AWS ElastiCache Parameter Group to apply. If not specified, the AWS default parameter group for the selected `redisVersion` will be used.                                                                                                                                 |
| `preferredCacheClusterAZs`    | string[]                               | No       | List of EC2 Availability Zones for placing cache nodes. Number of AZs should match total node count for even distribution. **Production:** Specify AZs close to application servers to minimize latency.                                                                                  |
| `preferredMaintenanceWindow`  | string                                 | No       | The weekly time range (in UTC) for system maintenance. Format: `ddd:hh:mm-ddd:hh:mm` (e.g., `sun:04:00-sun:05:00`). Minimum window is 60 minutes. If not specified, AWS will assign one.                                                                                                  |
| `replicasPerNodeGroup`        | number                                 | No       | The number of read replicas per shard. Replicas enable high availability and read scaling but increase costs. Valid range: 0-5. Set to 1 or more for production workloads. **Default: `0`** (no replicas, minimizing cost for development/testing).                                       |
| `replicationGroupDescription` | string                                 | No       | Human-readable description of the replication group. Used for AWS resource management and documentation. **Default: `'ElastiCache Redis replication group'`**.                                                                                                                            |
| `snapshotRetentionLimit`      | number                                 | No       | The number of days to retain automatic backups. Backups incur storage costs. Valid range: 0-35 days. Set to 7-30 for production workloads. **Default: `0`** (no backups, minimizing storage costs for development/testing).                                                               |
| `snapshotWindow`              | string                                 | No       | Daily UTC time range for automated backups. Format: `HH:MM-HH:MM` (e.g., `03:00-05:00`). Minimum 60-minute window. Required when `snapshotRetentionLimit > 0`. If not specified and snapshots are enabled, AWS assigns a window. **Production:** Set during low-traffic hours.            |
| `tags`                        | [object](#tags)                        | No       | A key-value map of AWS tags to apply to all created resources for cost tracking, automation, and organization. **Default: `{}`** (no tags).                                                                                                                                               |
| `timeout`                     | number                                 | No       | The client idle timeout in seconds. Set a non-zero value to automatically close idle clients. **Default: `0`** (disabled, suitable for long-lived connections).                                                                                                                           |
| `transitEncryptionEnabled`    | boolean                                | No       | Enables TLS to encrypt data in transit between clients and the Redis server. Enable for production workloads handling sensitive data. Note: may impact performance. **Default: `false`** (disabled to simplify client configuration for development/testing).                             |
| `userGroupIds`                | string[]                               | No       | List of user group IDs for RBAC (Redis 6.0+). Each group defines users and their permissions. Only works when authentication is enabled. **Production:** Create groups like 'readonly', 'admin', 'app-specific' with appropriate ACL rules.                                               |

### logDeliveryConfigurations

#### Properties

| Property             | Type                          | Required | Description                                                                             |
|----------------------|-------------------------------|----------|-----------------------------------------------------------------------------------------|
| `destinationDetails` | [object](#destinationdetails) | **Yes**  | Destination-specific configuration (log group for CloudWatch, stream for Kinesis)       |
| `destinationType`    | string                        | **Yes**  | AWS service to send logs to Possible values are: `cloudwatch-logs`, `kinesis-firehose`. |
| `logType`            | string                        | **Yes**  | Type of Redis log to capture Possible values are: `slow-log`, `engine-log`.             |
| `enabled`            | boolean                       | No       | Whether this log configuration is enabled                                               |

#### destinationDetails

Destination-specific configuration (log group for CloudWatch, stream for Kinesis)

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### tags

A key-value map of AWS tags to apply to all created resources for cost tracking, automation, and organization. **Default: `{}`** (no tags).

| Property | Type | Required | Description |
|----------|------|----------|-------------|



## Configuration Examples

### Minimal Development Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678"]
}
```
This creates a single `cache.t4g.micro` node with no replicas, no encryption, and no backups - perfect for development at minimal cost.

### Production-Ready Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678", "sg-87654321"],
  "cacheNodeType": "cache.r6g.large",
  "replicasPerNodeGroup": 2,
  "automaticFailoverEnabled": true,
  "multiAzEnabled": true,
  "transitEncryptionEnabled": true,
  "atRestEncryptionEnabled": true,
  "snapshotRetentionLimit": 7
}
```
This creates a highly available setup with encryption, automatic failover, and backups.

### Redis Cluster Mode Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678"],
  "numNodeGroups": 3,
  "replicasPerNodeGroup": 1,
  "cacheNodeType": "cache.r6g.xlarge",
  "automaticFailoverEnabled": true,
  "multiAzEnabled": true
}
```
This creates a sharded Redis cluster for horizontal scaling.

## Production Deployment Recommendations

When deploying to production, consider enabling these features:

1. **High Availability**: Set `replicasPerNodeGroup: 1` or higher and `automaticFailoverEnabled: true`
2. **Multi-AZ Resilience**: Enable `multiAzEnabled: true` to spread nodes across availability zones
3. **Encryption in Transit**: Enable `transitEncryptionEnabled: true` for sensitive data
4. **Encryption at Rest**: Enable `atRestEncryptionEnabled: true` for compliance requirements
5. **Automated Backups**: Set `snapshotRetentionLimit: 7` or higher for disaster recovery
6. **Network Security**: Use restrictive security groups and private subnets
7. **Right-size Instances**: Choose appropriate `cacheNodeType` based on workload analysis

## AWS Resources Created

When deployed, this flavor creates:
- ElastiCache Replication Group or Cluster
- Cache Parameter Group (if specified)
- Automatic snapshots based on retention policy
- CloudWatch metrics and alarms (if configured)