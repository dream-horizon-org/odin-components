package com.dream11.redis.state;

import com.dream11.redis.config.user.DeployConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {
  long version;
  String identifier;
  DeployConfig deployConfig;

  String replicationGroupIdentifier;
  String writerInstanceIdentifier;
  Map<String, List<String>> readerInstanceIdentifiers;

  String cacheParameterGroupName;

  String writerEndpoint;
  String readerEndpoint;

  public void incrementVersion() {
    this.version++;
  }
}
