## Container Flavor

Deploy your MySQL in container

### Container provisioning configuration

#### Properties

| Property          | Type                 | Required | Description                                                         |
|-------------------|----------------------|----------|---------------------------------------------------------------------|
| `imageRegistry`   | string               | **Yes**  | The image registry from which the docker image needs to be pulled   |
| `imageRepository` | string               | **Yes**  | The image repository from which the docker image needs to be pulled |
| `nodeSelector`    | string               | **Yes**  | Node labels for MySQL pods assignment                               |
| `readerCount`     | integer              | **Yes**  | Number of MySQL reader replicas                                     |
| `resources`       | [object](#resources) | **Yes**  |                                                                     |
| `storageClass`    | string               | **Yes**  | MySQL persistent volume storage class                               |

#### resources

##### Properties

| Property   | Type                | Required | Description                                 |
|------------|---------------------|----------|---------------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | The resources limits for MySQL containers   |
| `requests` | [object](#requests) | **Yes**  | The resources requests for MySQL containers |

##### limits

The resources limits for MySQL containers

###### Properties

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

##### requests

The resources requests for MySQL containers

###### Properties

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |


