# odin-redis-component

Deploy and manage Redis on various platforms.

## Flavors
- [aws_elasticache](aws_elasticache)

## Redis Component Definition

### Properties

| Property             | Type                      | Required | Description                                                                                                                                                                                                                                                                                                                                                        |
|----------------------|---------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `authentication`     | [object](#authentication) | **Yes**  | Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.                                                                                                                                                                                                                                 |
| `discovery`          | [object](#discovery)      | **Yes**  | Defines the logical DNS names where the Redis service will be accessible.                                                                                                                                                                                                                                                                                          |
| `redisVersion`       | string                    | **Yes**  | The version of the Redis engine to use. Supported versions: 7.1 (latest), 7.0, 6.2, 6.0, 5.0.6. Newer versions provide better performance and features. Must be explicitly provided. Possible values are: `7.1`, `7.0`, `6.2`, `6.0`, `5.0.6`.                                                                                                                     |
| `clusterModeEnabled` | boolean                   | No       | Specifies whether to run in Redis Cluster mode for sharding and horizontal scaling. For large datasets, enabling this is recommended. **Default: `false`** (single-shard setup).                                                                                                                                                                                   |
| `maxmemoryPolicy`    | string                    | No       | The eviction policy Redis uses when `maxmemory` is reached. This is a key parameter for tuning cache behavior. **Default: `allkeys-lru`** (evicts any key using LRU, effective for general-purpose caching). Possible values are: `volatile-lru`, `allkeys-lru`, `volatile-lfu`, `allkeys-lfu`, `volatile-random`, `allkeys-random`, `volatile-ttl`, `noeviction`. |

### authentication

Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.

#### Properties

| Property    | Type    | Required | Description                                                                                                                        |
|-------------|---------|----------|------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`   | boolean | **Yes**  | Set to `true` to enable password protection. **Default: `false`** (disabled for simplicity in trusted environments).               |
| `authToken` | string  | No       | The secret password to use when authentication is enabled. This value must be provided by the user and should be managed securely. |

### discovery

Defines the logical DNS names where the Redis service will be accessible.

#### Properties

| Property           | Type   | Required | Description                                                                                                                                                  |
|--------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `primaryEndpoint`  | string | **Yes**  | The logical DNS name for the primary (read/write) endpoint. This is the entry point for all write operations. Must be provided by the user.                  |
| `readOnlyEndpoint` | string | No       | The logical DNS name for the read-only endpoint, which directs traffic to replicas. Using this endpoint is a best practice for scaling read-heavy workloads. |



## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh stage=deploy account_flavour=dev_aws_elasticache
```

## Contributing

* Run `bash readme-generator.sh` to auto generate README