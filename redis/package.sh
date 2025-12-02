#!/usr/bin/env bash
set -euo pipefail

cd aws_elasticache
mvn --no-transfer-progress clean package -DskipTests
rm -rf src/ target/ pom.xml lombok.config README.md.tpl

cd ..
