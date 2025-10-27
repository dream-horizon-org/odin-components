package com.dream11.redis.service;

import com.dream11.redis.Application;
import com.dream11.redis.client.RedisClient;
import com.dream11.redis.config.metadata.ComponentMetadata;
import com.dream11.redis.config.metadata.aws.AwsAccountData;
import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import com.dream11.redis.util.ApplicationUtil;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RedisService {
  @NonNull final DeployConfig deployConfig;
  @NonNull final ComponentMetadata componentMetadata;
  @NonNull final RedisClient redisClient;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final RedisData redisData;

  public void deploy() {
    List<Callable<Void>> tasks = new ArrayList<>();
    String identifier = Application.getState().getIdentifier();
    if (identifier == null) {
      identifier = ApplicationUtil.generateRandomId(4);
      Application.getState().setIdentifier(identifier);
    }
    String name =
        ApplicationUtil.joinByHyphen(
            this.componentMetadata.getComponentName(), this.componentMetadata.getEnvName());

    Map<String, String> tags =
        ApplicationUtil.merge(
            List.of(
                this.deployConfig.getTags(),
                this.awsAccountData.getTags(),
                Constants.COMPONENT_TAGS));

    String cacheParameterGroupName = this.createCacheParameterGroup(name, identifier, tags);

    String clusterIdentifier =
        this.createReplicationGroupAndWait(name, identifier, tags, cacheParameterGroupName);

    tasks.addAll(
        this.createWriterInstanceAndWaitTaks(
            name, identifier, clusterIdentifier, tags, instanceParameterGroupName));
    tasks.addAll(
        this.createReaderInstancesAndWaitTaks(
            name, identifier, clusterIdentifier, tags, instanceParameterGroupName));

    ApplicationUtil.runOnExecutorService(tasks);

    log.info("redis cluster deployment completed successfully");
  }

  private String createCacheParameterGroup(
      String name, String identifier, Map<String, String> tags) {
    String cacheParameterGroupName =
        this.deployConfig.getCacheParameterGroupName() != null
            ? this.deployConfig.getCacheParameterGroupName()
            : Application.getState().getCacheParameterGroupName();
    if (cacheParameterGroupName == null) {
      cacheParameterGroupName =
          ApplicationUtil.joinByHyphen(name, Constants.CACHE_PARAMETER_GROUP_SUFFIX, identifier);
      log.info("Creating cache parameter group: {}", cacheParameterGroupName);
      this.redisClient.createCacheParameterGroup(
          cacheParameterGroupName, this.deployConfig.getVersion(), tags);

      Application.getState().setCacheParameterGroupName(cacheParameterGroupName);
    }

    return cacheParameterGroupName;
  }

  private String createReplicationGroupAndWait(
      String name, String identifier, Map<String, String> tags, String cacheParameterGroupName) {
    String replicationGroupIdentifier = Application.getState().getReplicationGroupIdentifier();
    if (replicationGroupIdentifier == null) {
      replicationGroupIdentifier = ApplicationUtil.joinByHyphen(name, identifier);
      List<String> endpoints;


        log.info("Creating Replication group: {}", replicationGroupIdentifier);
        endpoints =
            this.redisClient.createReplicationGroupFromScratch(
                    replicationGroupIdentifier,
                cacheParameterGroupName,
                tags,
                this.deployConfig,
                this.redisData);

      Application.getState().setReplicationGroupIdentifier(replicationGroupIdentifier);
      Application.getState().setWriterEndpoint(endpoints.get(0));
      Application.getState().setReaderEndpoint(endpoints.get(1));
      log.info("Waiting for Replication group to become available: {}", replicationGroupIdentifier);
      this.redisClient.waitUntilDBClusterAvailable(replicationGroupIdentifier);
      log.info("Replication group is now available: {}", replicationGroupIdentifier);
    }
    return replicationGroupIdentifier;
  }

  private List<Callable<Void>> createWriterInstanceAndWaitTaks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String instanceId = ApplicationUtil.generateRandomId(4);
      String writerInstanceIdentifier = ApplicationUtil.joinByHyphen(name, instanceId, identifier);
      log.info("Creating DB writer instance: {}", writerInstanceIdentifier);
      this.redisClient.createDBInstance(
          writerInstanceIdentifier,
          clusterIdentifier,
          instanceParameterGroupName,
          tags,
          this.deployConfig.getWriter().getInstanceType(),
          this.deployConfig.getWriter().getPromotionTier(),
          this.deployConfig.getInstanceConfig());
      Application.getState().setWriterInstanceIdentifier(writerInstanceIdentifier);
      tasks.add(
          () -> {
            log.info(
                "Waiting for DB writer instance to become available: {}", writerInstanceIdentifier);
            this.rdsClient.waitUntilDBInstanceAvailable(writerInstanceIdentifier);
            log.info("DB writer instance is now available: {}", writerInstanceIdentifier);
            return null;
          });
    }
    return tasks;
  }

  private List<Callable<Void>> createReaderInstancesAndWaitTaks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (this.deployConfig.getReaders() != null && !this.deployConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }

      for (int i = 0; i < this.deployConfig.getReaders().size(); i++) {
        String instanceType = this.deployConfig.getReaders().get(i).getInstanceType();
        Integer promotionTier = this.deployConfig.getReaders().get(i).getPromotionTier();
        Integer instanceCount = this.deployConfig.getReaders().get(i).getInstanceCount();

        List<String> existingInstances =
            Application.getState().getReaderInstanceIdentifiers().get(instanceType);
        Integer stateInstanceCount = existingInstances != null ? existingInstances.size() : 0;

        for (int j = stateInstanceCount; j < instanceCount; j++) {
          String instanceId = ApplicationUtil.generateRandomId(4);
          String readerInstanceIdentifier =
              ApplicationUtil.joinByHyphen(name, instanceId, identifier);
          log.info("Creating redis reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.createDBInstance(
              readerInstanceIdentifier,
              clusterIdentifier,
              instanceParameterGroupName,
              tags,
              instanceType,
              promotionTier,
              this.deployConfig.getInstanceConfig());
          Application.getState()
              .getReaderInstanceIdentifiers()
              .computeIfAbsent(instanceType, k -> new ArrayList<>())
              .add(readerInstanceIdentifier);

          tasks.add(
              () -> {
                log.info(
                    "Waiting for DB reader instance to become available: {}",
                    readerInstanceIdentifier);
                this.rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                log.info("DB reader instance is now available: {}", readerInstanceIdentifier);
                return null;
              });
        }
      }
    }

    return tasks;
  }



  @SneakyThrows
  private void waitUntilDBClusterFailover(String clusterIdentifier) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() < startTime + Constants.DB_WAIT_RETRY_TIMEOUT.toMillis()) {
      String status = this.rdsClient.getDBCluster(clusterIdentifier).status();
      log.debug("DB cluster {} status: {}", clusterIdentifier, status);
      if ("failing-over".equals(status)) {
        return;
      }
      Thread.sleep(Constants.DB_WAIT_RETRY_INTERVAL.toMillis());
    }
    throw new GenericApplicationException(
        ApplicationError.DB_WAIT_TIMEOUT, "cluster", clusterIdentifier, "failover");
  }


  private String handleClusterParameterGroupUpdate(String clusterIdentifier) {
    if (Application.getState().getDeployConfig().getClusterParameterGroupName() != null) {
      if (this.updateClusterConfig.getClusterParameterGroupName() != null) {
        log.info(
            "Switching cluster parameter group from {} to {}",
            Application.getState().getDeployConfig().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupName());
        Application.getState()
            .setClusterParameterGroupName(this.updateClusterConfig.getClusterParameterGroupName());
        return this.updateClusterConfig.getClusterParameterGroupName();
      } else {
        throw new GenericApplicationException(
            ApplicationError.CANNOT_MODIFY_PARAMETER_GROUP_CONFIG);
      }
    } else {
      if (this.updateClusterConfig.getClusterParameterGroupConfig() != null) {
        log.info(
            "Updating cluster parameter group configuration: {}",
            Application.getState().getClusterParameterGroupName());
        this.rdsClient.configureDBClusterParameters(
            Application.getState().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupConfig());
        return null;
      } else if (this.updateClusterConfig.getClusterParameterGroupName() != null
          && !this.updateClusterConfig
              .getClusterParameterGroupName()
              .equals(Application.getState().getClusterParameterGroupName())) {
        log.info(
            "Switching cluster parameter group from {} to {}",
            Application.getState().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupName());
        Application.getState()
            .setClusterParameterGroupName(this.updateClusterConfig.getClusterParameterGroupName());
        return this.updateClusterConfig.getClusterParameterGroupName();
      }
    }

    return null;
  }

  private String handleInstanceParameterGroupUpdate() {
    String oldParameterGroupName =
        Application.getState().getDeployConfig().getInstanceConfig() != null
            ? Application.getState()
                .getDeployConfig()
                .getInstanceConfig()
                .getInstanceParameterGroupName()
            : null;
    String newParameterGroupName =
        this.updateClusterConfig.getInstanceConfig() != null
            ? this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupName()
            : null;
    InstanceParameterGroupConfig newParameterGroupConfig =
        this.updateClusterConfig.getInstanceConfig() != null
            ? this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupConfig()
            : null;

    if (oldParameterGroupName != null) {
      if (newParameterGroupName != null) {
        log.info(
            "Switching instance parameter group from {} to {}",
            oldParameterGroupName,
            newParameterGroupName);
        Application.getState().setInstanceParameterGroupName(newParameterGroupName);
        return newParameterGroupName;
      } else {
        throw new GenericApplicationException(
            ApplicationError.CANNOT_MODIFY_PARAMETER_GROUP_CONFIG);
      }
    } else {
      if (newParameterGroupConfig != null) {
        log.info(
            "Updating instance parameter group configuration: {}",
            Application.getState().getInstanceParameterGroupName());
        this.rdsClient.configureDBInstanceParameters(
            Application.getState().getInstanceParameterGroupName(), newParameterGroupConfig);
        return null;
      } else if (newParameterGroupName != null
          && !newParameterGroupName.equals(
              Application.getState().getInstanceParameterGroupName())) {
        log.info(
            "Switching instance parameter group from {} to {}",
            Application.getState().getInstanceParameterGroupName(),
            newParameterGroupName);
        Application.getState().setInstanceParameterGroupName(newParameterGroupName);
        return newParameterGroupName;
      }
    }

    return null;
  }

  private void handleTagUpdates(String clusterIdentifier) {
    Map<String, String> tags = this.updateClusterConfig.getTags();

    String clusterArn = this.rdsClient.getDBCluster(clusterIdentifier).dbClusterArn();
    this.rdsClient.mergeTagsForResource(clusterArn, tags);

    String writerInstanceIdentifier = Application.getState().getWriterInstanceIdentifier();
    if (writerInstanceIdentifier != null) {
      String writerArn = this.rdsClient.getDBInstance(writerInstanceIdentifier).dbInstanceArn();
      this.rdsClient.mergeTagsForResource(writerArn, tags);
    }

    Map<String, List<String>> readerInstanceIdentifiers =
        Application.getState().getReaderInstanceIdentifiers();
    if (readerInstanceIdentifiers != null) {
      for (Map.Entry<String, List<String>> entry : readerInstanceIdentifiers.entrySet()) {
        for (String readerInstanceIdentifier : entry.getValue()) {
          String readerArn = this.rdsClient.getDBInstance(readerInstanceIdentifier).dbInstanceArn();
          this.rdsClient.mergeTagsForResource(readerArn, tags);
        }
      }
    }

    String clusterParameterGroupName = Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName != null) {
      String clusterParamGroupArn =
          this.rdsClient
              .getDBClusterParameterGroup(clusterParameterGroupName)
              .dbClusterParameterGroupArn();
      this.rdsClient.mergeTagsForResource(clusterParamGroupArn, tags);
    }

    String instanceParameterGroupName = Application.getState().getInstanceParameterGroupName();
    if (instanceParameterGroupName != null) {
      String instanceParamGroupArn =
          this.rdsClient.getDBParameterGroup(instanceParameterGroupName).dbParameterGroupArn();
      this.rdsClient.mergeTagsForResource(instanceParamGroupArn, tags);
    }
  }

  public void undeploy() {
    List<Callable<Void>> tasks = new ArrayList<>();

    tasks.addAll(this.deleteReaderInstancesAndWaitTasks());
    tasks.addAll(this.deleteWriterInstanceAndWaitTasks());

    ApplicationUtil.runOnExecutorService(tasks);

    this.deleteClusterAndWait();

    this.deleteParameterGroups();

    log.info("redis cluster undeployment completed successfully");
  }

  private List<Callable<Void>> deleteReaderInstancesAndWaitTasks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (Application.getState().getReaderInstanceIdentifiers() != null) {
      List<Map.Entry<String, List<String>>> readerInstancesToDelete =
          new ArrayList<>(Application.getState().getReaderInstanceIdentifiers().entrySet());
      for (Map.Entry<String, List<String>> entry : readerInstancesToDelete) {
        List<String> readerInstanceIdentifiers = entry.getValue();
        String key = entry.getKey();
        for (String readerInstanceIdentifier : readerInstanceIdentifiers) {
          log.info("Deleting DB reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.deleteDBInstance(
              readerInstanceIdentifier,
              Application.getState().getDeployConfig().getDeletionConfig());
          tasks.add(
              () -> {
                log.info(
                    "Waiting for DB reader instance to become deleted: {}",
                    readerInstanceIdentifier);
                this.rdsClient.waitUntilDBInstanceDeleted(readerInstanceIdentifier);
                log.info("DB reader instance is now deleted: {}", readerInstanceIdentifier);
                Application.getState().getReaderInstanceIdentifiers().remove(key);
                return null;
              });
        }
      }
    }
    return tasks;
  }

  private List<Callable<Void>> deleteWriterInstanceAndWaitTasks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (Application.getState().getWriterInstanceIdentifier() != null) {
      log.info(
          "Deleting DB writer instance: {}", Application.getState().getWriterInstanceIdentifier());
      this.rdsClient.deleteDBInstance(
          Application.getState().getWriterInstanceIdentifier(),
          Application.getState().getDeployConfig().getDeletionConfig());
      tasks.add(
          () -> {
            log.info(
                "Waiting for DB writer instance to become deleted: {}",
                Application.getState().getWriterInstanceIdentifier());
            this.rdsClient.waitUntilDBInstanceDeleted(
                Application.getState().getWriterInstanceIdentifier());
            log.info(
                "DB writer instance is now deleted: {}",
                Application.getState().getWriterInstanceIdentifier());
            Application.getState().setWriterInstanceIdentifier(null);
            return null;
          });
    }
    return tasks;
  }

  private void deleteClusterAndWait() {
    if (Application.getState().getClusterIdentifier() != null) {
      log.info("Deleting DB cluster: {}", Application.getState().getClusterIdentifier());
      this.rdsClient.deleteDBCluster(
          Application.getState().getClusterIdentifier(),
          Application.getState().getDeployConfig().getDeletionConfig());
      log.info(
          "Waiting for DB cluster to become deleted: {}",
          Application.getState().getClusterIdentifier());
      this.rdsClient.waitUntilDBClusterDeleted(Application.getState().getClusterIdentifier());
      log.info("DB cluster is now deleted: {}", Application.getState().getClusterIdentifier());
      Application.getState().setClusterIdentifier(null);
      Application.getState().setWriterEndpoint(null);
      Application.getState().setReaderEndpoint(null);
    }
  }

  private void deleteParameterGroups() {
    if (Application.getState().getClusterParameterGroupName() != null) {
      log.info(
          "Deleting DB cluster parameter group: {}",
          Application.getState().getClusterParameterGroupName());
      this.rdsClient.deleteDBClusterParameterGroup(
          Application.getState().getClusterParameterGroupName());
      Application.getState().setClusterParameterGroupName(null);
    }
    if (Application.getState().getInstanceParameterGroupName() != null) {
      log.info(
          "Deleting DB instance parameter group: {}",
          Application.getState().getInstanceParameterGroupName());
      this.rdsClient.deleteDBParameterGroup(Application.getState().getInstanceParameterGroupName());
      Application.getState().setInstanceParameterGroupName(null);
    }
  }
}
