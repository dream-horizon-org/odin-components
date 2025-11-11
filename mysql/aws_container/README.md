## Container Flavor

Deploy your MySQL in container

### Container provisioning configuration

#### Properties

| Property          | Type              | Required | Description                                                         |
|-------------------|-------------------|----------|---------------------------------------------------------------------|
| `imageRegistry`   | string            | **Yes**  | The image registry from which the docker image needs to be pulled   |
| `imageRepository` | string            | **Yes**  | The image repository from which the docker image needs to be pulled |
| `imageTag`        | string            | **Yes**  | The image tag for mysql docker image                                |
| `reader`          | [object](#reader) | **Yes**  |                                                                     |
| `storageClass`    | string            | **Yes**  | MySQL persistent volume storage class                               |
| `writer`          | [object](#writer) | **Yes**  |                                                                     |
| `binlog`          | [object](#binlog) | No       | MySQL binary logging configuration                                  |
| `password`        | string            | No       | password to connect to mysql                                        |
| `username`        | string            | No       | username to connect to mysql                                        |

#### binlog

MySQL binary logging configuration

##### Properties

| Property        | Type                     | Required | Description                                |
|-----------------|--------------------------|----------|--------------------------------------------|
| `enabled`       | boolean                  | **Yes**  | Enable or disable MySQL binary logging     |
| `configuration` | [object](#configuration) | No       | Additional binlog configuration parameters |

##### configuration

Additional binlog configuration parameters

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### reader

##### Properties

| Property       | Type                     | Required | Description                                  |
|----------------|--------------------------|----------|----------------------------------------------|
| `resources`    | [object](#resources)     | **Yes**  |                                              |
| `nodeSelector` | [object](#nodeselector)  | No       | Node labels for MySQL writer pods assignment |
| `persistence`  | [object](#persistence)   | No       |                                              |
| `replicaCount` | integer                  | No       | Number of MySQL reader replicas              |
| `tolerations`  | [object](#tolerations)[] | No       | Tolerations for MySQL writer pods            |

##### nodeSelector

Node labels for MySQL writer pods assignment

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### persistence

###### Properties

| Property | Type   | Required | Description                                    |
|----------|--------|----------|------------------------------------------------|
| `size`   | string | **Yes**  | Size of the persistent volume for MySQL writer |

##### resources

###### Properties

| Property   | Type                | Required | Description                                        |
|------------|---------------------|----------|----------------------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | The resources limits for MySQL writer containers   |
| `requests` | [object](#requests) | **Yes**  | The resources requests for MySQL writer containers |

###### limits

The resources limits for MySQL writer containers

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

###### requests

The resources requests for MySQL writer containers

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

##### tolerations

###### Properties

| Property            | Type    | Required | Description                                                         |
|---------------------|---------|----------|---------------------------------------------------------------------|
| `key`               | string  | **Yes**  |                                                                     |
| `operator`          | string  | **Yes**  | Possible values are: `Equal`, `Exists`.                             |
| `effect`            | string  | No       | Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `tolerationSeconds` | integer | No       |                                                                     |
| `value`             | string  | No       |                                                                     |

#### writer

##### Properties

| Property       | Type                     | Required | Description                                  |
|----------------|--------------------------|----------|----------------------------------------------|
| `resources`    | [object](#resources)     | **Yes**  |                                              |
| `nodeSelector` | [object](#nodeselector)  | No       | Node labels for MySQL writer pods assignment |
| `persistence`  | [object](#persistence)   | No       |                                              |
| `tolerations`  | [object](#tolerations)[] | No       | Tolerations for MySQL writer pods            |

##### nodeSelector

Node labels for MySQL writer pods assignment

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### persistence

###### Properties

| Property | Type   | Required | Description                                    |
|----------|--------|----------|------------------------------------------------|
| `size`   | string | **Yes**  | Size of the persistent volume for MySQL writer |

##### resources

###### Properties

| Property   | Type                | Required | Description                                        |
|------------|---------------------|----------|----------------------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | The resources limits for MySQL writer containers   |
| `requests` | [object](#requests) | **Yes**  | The resources requests for MySQL writer containers |

###### limits

The resources limits for MySQL writer containers

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

###### requests

The resources requests for MySQL writer containers

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

##### tolerations

###### Properties

| Property            | Type    | Required | Description                                                         |
|---------------------|---------|----------|---------------------------------------------------------------------|
| `key`               | string  | **Yes**  |                                                                     |
| `operator`          | string  | **Yes**  | Possible values are: `Equal`, `Exists`.                             |
| `effect`            | string  | No       | Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `tolerationSeconds` | integer | No       |                                                                     |
| `value`             | string  | No       |                                                                     |


