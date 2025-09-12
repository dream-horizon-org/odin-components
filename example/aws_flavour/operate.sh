#!/usr/bin/env bash
set -euo pipefail

if [[ "$1" == "deploy" ]]; then
  echo "::info::Deploying my_flavour"
  if [[ "{{ flavourConfig.error }}" == "true" ]]; then
    echo "::error::Error while deploying my_flavour"
    exit 1
  fi
fi

if [[ "$1" == "undeploy" ]]; then
  echo "::info::Undeploying my_flavour"
fi
