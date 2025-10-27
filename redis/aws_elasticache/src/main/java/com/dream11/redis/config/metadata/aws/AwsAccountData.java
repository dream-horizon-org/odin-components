package com.dream11.redis.config.metadata.aws;

import com.dream11.redis.config.Config;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AwsAccountData implements Config {
  @NotBlank String region;
  @NotNull Map<String, String> tags = new HashMap<>();
}
