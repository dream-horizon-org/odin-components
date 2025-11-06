#!/usr/bin/env bash

function print_marker() {
  echo "=========================================================================================="
}

export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
export RELEASE_NAME={{ componentMetadata.name }}
export NAMESPACE={{ componentMetadata.envName }}
export BASE_VERSION={{ baseConfig.version }}
export IMAGE_TAG=$(jq -r --arg ver "$BASE_VERSION" '.[$ver]' versions.json)
if [[ "${IMAGE_TAG}" == "null" ]]; then
    echo "ERROR: imageTag not found for BASE_VERSION ${BASE_VERSION}" 1>&2
    exit 1
fi
echo "PREVIOUS_SHA:${PREVIOUS_SHA}"
CURRENT_SHA=$(sha256sum values.yaml)
if [[ "${CURRENT_SHA}" == "${PREVIOUS_SHA}" ]]; then
  echo "No changes to apply"
else
  helm upgrade --install ${RELEASE_NAME} oci://666019485799.dkr.ecr.us-east-1.amazonaws.com/bitnami/mysql --version 9.4.6 -n ${NAMESPACE} --values values.yaml --set image.tag=${IMAGE_TAG} --wait
  if [[ $? -ne 0 ]]; then
    echo "Mysql deployment failed. Please find pod description and logs below." 1>&2

    print_marker
    echo "Following pods were found"
    kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

    print_marker
    echo "Pod descriptions"
    kubectl describe pods -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

    print_marker
    echo "Pod logs"
    print_marker
    kubectl logs --since 5m -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

    # Exit with non zero error code
    exit 1
  fi
fi
