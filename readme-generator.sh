#!/usr/bin/env bash
set -euo pipefail

# Installs readme generator tool
go install github.com/marcusolsson/json-schema-docs@v0.2.1

# Function to generate README for a component
generate_component_readme() {
  local component_dir="${1}"

  echo "Processing component: ${component_dir}"

  # Generate component root level README if schema.json exists
  if [[ -f "${component_dir}/schema.json" && -f "${component_dir}/README.md.tpl" ]]; then
    echo "  Generating README for ${component_dir}"
    json-schema-docs -schema "${component_dir}/schema.json" -template "${component_dir}/README.md.tpl" > "${component_dir}/README.md"
  fi

  # Generate READMEs for flavors within the component
  for flavor_dir in "${component_dir}"/*; do
    if [[ ! -d "${flavor_dir}" || ! -f "${flavor_dir}/schema.json" ]]; then
      continue
    fi

    local flavor_name
    flavor_name=$(basename "${flavor_dir}")

    if [[ -f "${flavor_dir}/README.md.tpl" ]]; then
      echo "  Generating README for flavor: ${component_dir}/${flavor_name}"
      json-schema-docs -schema "${flavor_dir}/schema.json" -template "${flavor_dir}/README.md.tpl" > "${flavor_dir}/README.md"
    fi
  done
}

# Main execution
echo "Starting README generation for all components..."

# Process all component directories
for component in */; do
  # Skip hidden directories and non-directories
  if [[ ! -d "${component}" || "${component}" == ".*" ]]; then
    continue
  fi

  # Remove trailing slash
  component_name="${component%/}"

  # Skip directories that are clearly not components
  if [[ "${component_name}" == "target" || "${component_name}" == "node_modules" || "${component_name}" == ".git" ]]; then
    continue
  fi

  # Check if it's a component directory (has schema.json)
  if [[ -f "${component_name}/schema.json" ]]; then
    generate_component_readme "${component_name}"
  fi
done

echo "README generation complete!"
