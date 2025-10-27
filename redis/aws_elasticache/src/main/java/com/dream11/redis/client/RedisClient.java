package com.dream11.redis.client;

import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.config.user.LogDeliveryConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import com.dream11.redis.exception.ReplicationGroupNotFoundException;
import com.dream11.redis.util.ApplicationUtil;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheParameterGroupAlreadyExistsException;
import software.amazon.awssdk.services.elasticache.model.CreateCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.CreateCacheParameterGroupResponse;
import software.amazon.awssdk.services.elasticache.model.CreateReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;
import software.amazon.awssdk.services.elasticache.model.Tag;



@Slf4j
public class RedisClient {
  final ElastiCacheClient elastiCacheClient;

  public RedisClient(String region) {
    this.elastiCacheClient =
        ElastiCacheClient.builder()
            .region(Region.of(region))
            .overrideConfiguration(
                overrideConfig ->
                    overrideConfig
                        .retryStrategy(RetryMode.STANDARD)
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30)))
            .build();
  }

  /**
   * Creates an ElastiCache Redis replication group from scratch.
   *
   * @param replicationGroupId The identifier for the replication group
   * @param cacheParameterGroupName The name of the cache parameter group
   * @param tags Tags to apply to the replication group
   * @param deployConfig Configuration for deployment
   * @param redisData Metadata containing subnet groups and security groups
   * @return The primary endpoint of the replication group
   */
  public String createReplicationGroupFromScratch(
      String replicationGroupId,
      String cacheParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData) {

    CreateReplicationGroupRequest.Builder createBuilder =
        CreateReplicationGroupRequest.builder();

    // Apply common configuration
    applyCommonConfiguration(
        createBuilder,
        replicationGroupId,
        cacheParameterGroupName,
        tags,
        deployConfig,
        redisData);

    // Set engine version
    createBuilder.engineVersion(deployConfig.getVersion());

    CreateReplicationGroupRequest request = createBuilder.build();

    ReplicationGroup replicationGroup =
        this.elastiCacheClient.createReplicationGroup(request).replicationGroup();

    // Return the primary endpoint (configuration endpoint for cluster mode)
    if (replicationGroup.configurationEndpoint() != null) {
      return replicationGroup.configurationEndpoint().address() + ":" 
          + replicationGroup.configurationEndpoint().port();
    } else if (replicationGroup.nodeGroups() != null 
        && !replicationGroup.nodeGroups().isEmpty()
        && replicationGroup.nodeGroups().get(0).primaryEndpoint() != null) {
      return replicationGroup.nodeGroups().get(0).primaryEndpoint().address() + ":" 
          + replicationGroup.nodeGroups().get(0).primaryEndpoint().port();
    }
    
    return replicationGroupId;
  }

  /**
   * Helper method to apply common configuration to the replication group builder.
   */
  private void applyCommonConfiguration(
      CreateReplicationGroupRequest.Builder builder,
      String replicationGroupId,
      String cacheParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData) {

    // Set required fields
    builder.replicationGroupId(replicationGroupId);
    builder.replicationGroupDescription(deployConfig.getReplicationGroupDescription());
    builder.engine(Constants.ENGINE_TYPE);
    builder.cacheNodeType(deployConfig.getCacheNodeType());

    // Set parameter group if provided
    ApplicationUtil.setIfNotNull(builder::cacheParameterGroupName, cacheParameterGroupName);

    // Set subnet group from metadata
    if (redisData.getSubnetGroups() != null && !redisData.getSubnetGroups().isEmpty()) {
      builder.cacheSubnetGroupName(redisData.getSubnetGroups().get(0));
    }

    // Set security groups from metadata
    if (redisData.getSecurityGroups() != null && !redisData.getSecurityGroups().isEmpty()) {
      builder.securityGroupIds(redisData.getSecurityGroups());
    }

    // Set tags
    builder.tags(convertMapToTags(tags));

    // Set cluster configuration
    ApplicationUtil.setIfNotNull(builder::numNodeGroups, deployConfig.getNumNodeGroups());
    ApplicationUtil.setIfNotNull(
        builder::replicasPerNodeGroup, deployConfig.getReplicasPerNodeGroup());

    // Set optional boolean flags
    ApplicationUtil.setIfNotNull(
        builder::automaticFailoverEnabled, deployConfig.getAutomaticFailoverEnabled());
    ApplicationUtil.setIfNotNull(builder::multiAZEnabled, deployConfig.getMultiAzEnabled());
    ApplicationUtil.setIfNotNull(
        builder::transitEncryptionEnabled, deployConfig.getTransitEncryptionEnabled());
    ApplicationUtil.setIfNotNull(
        builder::atRestEncryptionEnabled, deployConfig.getAtRestEncryptionEnabled());

    // Set snapshot and backup configuration
    ApplicationUtil.setIfNotNull(
        builder::snapshotRetentionLimit, deployConfig.getSnapshotRetentionLimit());
    ApplicationUtil.setIfNotNull(builder::snapshotWindow, deployConfig.getSnapshotWindow());

    // Set maintenance window
    ApplicationUtil.setIfNotNull(
        builder::preferredMaintenanceWindow, deployConfig.getPreferredMaintenanceWindow());

    // Set notification topic
    ApplicationUtil.setIfNotNull(
        builder::notificationTopicArn, deployConfig.getNotificationTopicArn());

    // Set auto minor version upgrade
    ApplicationUtil.setIfNotNull(
        builder::autoMinorVersionUpgrade, deployConfig.getAutoMinorVersionUpgrade());

    // Set preferred AZs
    ApplicationUtil.setIfNotNull(
        builder::preferredCacheClusterAZs, deployConfig.getPreferredCacheClusterAZs());

    // Set KMS key for encryption
    ApplicationUtil.setIfNotNull(builder::kmsKeyId, deployConfig.getKmsKeyId());

    // Note: Log delivery configurations are handled separately if needed
    // as they may require specific AWS SDK version support
  }

  /**
   * Converts a map of tags to ElastiCache Tag objects.
   */
  private List<Tag> convertMapToTags(Map<String, String> tagMap) {
    return tagMap.entrySet().stream()
        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
        .collect(Collectors.toList());
  }

  /**
   * Retrieves a replication group by its identifier.
   *
   * @param replicationGroupId The identifier of the replication group
   * @return The ReplicationGroup object
   * @throws ReplicationGroupNotFoundException if the replication group is not found
   */
  public ReplicationGroup getReplicationGroup(String replicationGroupId) {
    DescribeReplicationGroupsRequest request =
        DescribeReplicationGroupsRequest.builder()
            .replicationGroupId(replicationGroupId)
            .build();

    return this.elastiCacheClient.describeReplicationGroups(request).replicationGroups().stream()
        .findFirst()
        .orElseThrow(() -> new ReplicationGroupNotFoundException(replicationGroupId));
  }


  public void createCacheParameterGroup(String cacheParameterGroupName, @NotNull String version, Map<String, String> tags) {
      elastiCacheClient.createCacheParameterGroup(
              CreateCacheParameterGroupRequest.builder()
                      .cacheParameterGroupName(cacheParameterGroupName)
                      .description(cacheParameterGroupName)
                      .cacheParameterGroupFamily(Constants.ENGINE_TYPE+version)
                      .tags(convertMapToTags(tags))
                      .build());



  }
}
