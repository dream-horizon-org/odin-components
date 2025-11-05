#!/bin/bash
set -e

export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
export RELEASE_NAME={{ componentMetadata.name }}
export NAMESPACE={{ componentMetadata.envName }}

function get_endpoints() {
  kubectl get endpoints -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME},app.kubernetes.io/component=$1 | grep headless | awk '{gsub(":[0-9]+", "", $2); print $2}'
}

WRITER_ENDPOINT=$(get_endpoints primary)

if [[ {{ readerCount }} -eq 0 ]]; then
  READER_ENDPOINT=${WRITER_ENDPOINT}
else
  READER_ENDPOINT=$(get_endpoints secondary)
fi
echo "{\"writer\":\"${WRITER_ENDPOINT}\", \"reader\":\"${READER_ENDPOINT}\"}"
