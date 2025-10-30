#!/usr/bin/env bash
set -euo pipefail

INPUT_FILE="index.yaml"
README_FILE="README.md"

mkdir -p components
echo "## Odin Components" > "${README_FILE}"
echo "" >> "${README_FILE}"

# Extract all component names and write markdown lines
yq -r '.components[][] | [.name, .description] | @tsv' "${INPUT_FILE}" | while read -r name description; do
  COMPONENT_FILE_NAME="components/${name}.md"
  # Create file for each component
  echo "## ${name} component versions" > "${COMPONENT_FILE_NAME}"
  echo "" >> "${COMPONENT_FILE_NAME}"
  yq -r ".components.\"${name}\".[] | [.version, .downloadUrl] | @tsv" "${INPUT_FILE}" | while read -r version downloadUrl; do
    echo "- [${version}](${downloadUrl})" >> "${COMPONENT_FILE_NAME}"
  done
  echo "- [${name}](${COMPONENT_FILE_NAME}): ${description}" >> "${README_FILE}"
done
