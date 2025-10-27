package com.dream11.redis.service;
import com.dream11.redis.client.RedisClient;

import com.dream11.redis.Application;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.state.State;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBClusterMember;
import software.amazon.awssdk.services.rds.model.DBInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateService {

  @NonNull final RedisClient redisClient;
  @NonNull
  DeployConfig deployConfig;

  public void reconcileState() {
    State state = Application.getState();

    if (state.getClusterIdentifier() != null) {
      try {
        DBCluster cluster = this.redisClient.getDBCluster(state.getClusterIdentifier());
        populateStateFromCluster(cluster, state);
        log.debug("Found cluster: {}", state.getClusterIdentifier());
      } catch (DBClusterNotFoundException ex) {
        log.warn(
            "DB cluster:[{}] from state does not exist. Updating state.",
            state.getClusterIdentifier());
        state.setClusterIdentifier(null);
        return;
      }
    }
  }

  private void populateStateFromCluster(DBCluster cluster, State state) {

    if (state.getReaderInstanceIdentifiers() == null) {
      state.setReaderInstanceIdentifiers(new HashMap<>());
    }

    state.setWriterInstanceIdentifier(null);
    state.getReaderInstanceIdentifiers().clear();
    Map<String, ReaderConfig> readers = new HashMap<>();

    for (DBClusterMember member : cluster.dbClusterMembers()) {
      String instanceIdentifier = member.dbInstanceIdentifier();
      DBInstance instance = this.rdsClient.getDBInstance(instanceIdentifier);
      String instanceType = instance.dbInstanceClass();
      Integer promotionTier = instance.promotionTier();

      if (member.isClusterWriter()) {
        state.setWriterInstanceIdentifier(instanceIdentifier);
        if (state.getDeployConfig() != null) {
          state
              .getDeployConfig()
              .setWriter(
                  WriterConfig.builder()
                      .instanceType(instanceType)
                      .promotionTier(promotionTier)
                      .build());
        }
        this.deployConfig.setWriter(
            WriterConfig.builder().instanceType(instanceType).promotionTier(promotionTier).build());
        log.debug("Found writer instance: {}", instanceIdentifier);
      } else {
        state
            .getReaderInstanceIdentifiers()
            .computeIfAbsent(instanceType, k -> new ArrayList<>())
            .add(instanceIdentifier);
        if (readers.containsKey(instanceType)) {
          readers
              .get(instanceType)
              .setInstanceCount(readers.get(instanceType).getInstanceCount() + 1);
        } else {
          readers.put(
              instanceType,
              ReaderConfig.builder()
                  .instanceCount(1)
                  .instanceType(instanceType)
                  .promotionTier(promotionTier)
                  .build());
        }
        log.debug("Found reader instance: {} of type: {}", instanceIdentifier, instanceType);
      }
    }
    if (state.getDeployConfig() != null) {
      state.getDeployConfig().setReaders(new ArrayList<>(readers.values()));
    }
    this.deployConfig.setReaders(new ArrayList<>(readers.values()));
  }
}
