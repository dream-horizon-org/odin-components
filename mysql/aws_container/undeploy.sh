#!/bin/bash

export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
export RELEASE_NAME={{ componentMetadata.name }}
export NAMESPACE={{ componentMetadata.envName }}

# Uninstalling helm chart
echo "Starting undeployment of MySQL ${RELEASE_NAME}" | log_with_timestamp
if helm uninstall ${RELEASE_NAME} -n ${NAMESPACE} 2> >(log_errors_with_timestamp); then
    echo "Helm release uninstalled successfully" | log_with_timestamp
else
    echo "Helm release not found or already deleted" | log_with_timestamp
fi

echo "Destroying persistent volume claims"
kubectl get pvc -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME} | awk '(NR>1){print $1}' | xargs kubectl delete pvc -n ${NAMESPACE}
