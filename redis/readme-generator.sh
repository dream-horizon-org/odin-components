#!/usr/bin/env bash
set -euo pipefail

# Installs readme generator tool
go install github.com/marcusolsson/json-schema-docs@v0.2.1

# Generate root level README
json-schema-docs -schema schema.json -template README.md.tpl > README.md

for flavor in *; do
  if [[ ! -d ${flavor} || ! -f ${flavor}/schema.json ]]; then
    continue
  fi
  # Generate flavor level README using its own template
  json-schema-docs -schema "${flavor}/schema.json" -template "${flavor}/README.md.tpl" > "${flavor}/README.md"
done
