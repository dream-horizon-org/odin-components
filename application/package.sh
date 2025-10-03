#!/usr/bin/env bash
set -euo pipefail

# Local kubernetes
cd local_kubernetes
mvn --no-transfer-progress clean package -DskipTests
rm -rf src/ target/ pom.xml lombok.config README.md.tpl

cd .. # Return to component directory
