#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
workspace_dir="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/setup_loop_workspace.sh [--workspace <path>]

Description:
  Initialize Agent Optsmith data directories for the target project.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --workspace)
      workspace_dir="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${workspace_dir}" ]]; then
  echo "error: workspace path is empty"
  exit 1
fi

workspace_dir="$(cd "${workspace_dir}" && pwd)"

data_root="${OPTSMITH_DATA_ROOT:-${workspace_dir}/.agents/optsmith-data}"
metrics_file="${OPTSMITH_DATA_FILE:-${data_root}/metrics/task-runs.csv}"
kb_dir="${OPTSMITH_KB_DIR:-${data_root}/knowledge-base/errors}"
report_dir="${OPTSMITH_REPORT_DIR:-${data_root}/reports}"
templates_dir="${data_root}/templates"
error_template="${templates_dir}/error-entry.md"
template_root="${SCRIPT_DIR}/../templates/workspace"
template_metrics="${template_root}/metrics/task-runs.csv"
template_error="${template_root}/templates/error-entry.md"

if [[ ! -d "${template_root}" ]]; then
  echo "error: template root not found: ${template_root}"
  exit 1
fi

if [[ ! -f "${template_metrics}" ]]; then
  echo "error: missing template file: ${template_metrics}"
  exit 1
fi

if [[ ! -f "${template_error}" ]]; then
  echo "error: missing template file: ${template_error}"
  exit 1
fi

mkdir -p "${data_root}"

while IFS= read -r rel_dir; do
  [[ -z "${rel_dir}" ]] && continue
  mkdir -p "${data_root}/${rel_dir}"
done < <(cd "${template_root}" && find . -type d | sed -E 's#^\./##')

while IFS= read -r rel_file; do
  [[ -z "${rel_file}" ]] && continue
  if [[ "${rel_file}" == *.gitkeep ]]; then
    continue
  fi
  src="${template_root}/${rel_file}"
  dst="${data_root}/${rel_file}"
  if [[ ! -f "${dst}" ]]; then
    mkdir -p "$(dirname "${dst}")"
    cp "${src}" "${dst}"
  fi
done < <(cd "${template_root}" && find . -type f | sed -E 's#^\./##')

mkdir -p "$(dirname "${metrics_file}")" "${kb_dir}" "${report_dir}" "${templates_dir}"

if [[ ! -f "${metrics_file}" ]]; then
  cp "${template_metrics}" "${metrics_file}"
fi

if [[ ! -f "${error_template}" ]]; then
  cp "${template_error}" "${error_template}"
fi

echo "initialized Agent Optsmith workspace:"
echo "  workspace: ${workspace_dir}"
echo "  data_root: ${data_root}"
echo "  metrics_file: ${metrics_file}"
echo "  kb_dir: ${kb_dir}"
echo "  report_dir: ${report_dir}"
echo "  error_template: ${error_template}"
echo "  template_root: ${template_root}"
