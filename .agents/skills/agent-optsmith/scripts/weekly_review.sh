#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKSPACE_DIR="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
WORKSPACE_DIR="$(cd "${WORKSPACE_DIR}" && pwd)"
KB_DIR_DEFAULT="${WORKSPACE_DIR}/.agents/optsmith-data/knowledge-base/errors"
REPORT_DIR_DEFAULT="${WORKSPACE_DIR}/.agents/optsmith-data/reports"
KB_DIR="${OPTSMITH_KB_DIR:-${KB_DIR_DEFAULT}}"
REPORT_DIR="${OPTSMITH_REPORT_DIR:-${REPORT_DIR_DEFAULT}}"

today="$(date +%Y-%m-%d)"
start_date="$(date -v-6d +%Y-%m-%d 2>/dev/null || date -d '6 days ago' +%Y-%m-%d)"
report_file="${REPORT_DIR}/${today}-weekly-agent-optsmith-report.md"

mkdir -p "${KB_DIR}" "${REPORT_DIR}"

total=0
resolved=0
token_sum=0
token_count=0

task_types_tmp="$(mktemp)"
root_causes_tmp="$(mktemp)"
trap 'rm -f "${task_types_tmp}" "${root_causes_tmp}"' EXIT

for file in "${KB_DIR}"/*.md; do
  [[ -e "${file}" ]] || continue

  entry_date="$(awk -F': ' '/^date: /{print $2; exit}' "${file}")"
  [[ -n "${entry_date}" ]] || continue

  if [[ "${entry_date}" < "${start_date}" || "${entry_date}" > "${today}" ]]; then
    continue
  fi

  total=$((total + 1))

  status="$(awk -F': ' '/^status: /{print $2; exit}' "${file}")"
  if [[ "${status}" == "closed" ]]; then
    resolved=$((resolved + 1))
  fi

  task_type="$(awk -F': ' '/^task_type: /{print $2; exit}' "${file}")"
  root_cause="$(awk -F': ' '/^root_cause: /{print $2; exit}' "${file}")"
  token_cost="$(awk -F': ' '/^token_cost_estimate: /{print $2; exit}' "${file}")"

  [[ -n "${task_type}" ]] && echo "${task_type}" >> "${task_types_tmp}"
  [[ -n "${root_cause}" ]] && echo "${root_cause}" >> "${root_causes_tmp}"

  if [[ "${token_cost}" =~ ^[0-9]+$ ]]; then
    token_sum=$((token_sum + token_cost))
    token_count=$((token_count + 1))
  fi
done

open=$((total - resolved))

if [[ "${token_count}" -gt 0 ]]; then
  avg_token=$((token_sum / token_count))
else
  avg_token=0
fi

top_root_causes="$(sort "${root_causes_tmp}" | uniq -c | sort -nr | head -n 3 | sed -E 's/^[[:space:]]*([0-9]+)[[:space:]]+/- \1x /')"
top_task_types="$(sort "${task_types_tmp}" | uniq -c | sort -nr | head -n 3 | sed -E 's/^[[:space:]]*([0-9]+)[[:space:]]+/- \1x /')"

[[ -n "${top_root_causes}" ]] || top_root_causes="- none"
[[ -n "${top_task_types}" ]] || top_task_types="- none"

cat > "${report_file}" <<EOF
# Weekly Agent Optsmith Report

## Scope

- period_start: ${start_date}
- period_end: ${today}

## KPI Snapshot

- total_incidents: ${total}
- resolved_incidents: ${resolved}
- open_incidents: ${open}
- avg_token_cost_estimate: ${avg_token}

## Top Root Causes

${top_root_causes}

## Top High-Cost Task Types

${top_task_types}

## Skill Actions

- skills_to_create:
- skills_to_refactor:
- skills_to_deprecate:

## AGENTS Actions

- rules_to_add:
- rules_to_update:
- rules_to_remove:

## Next Week Goals

1.
2.
3.
EOF

echo "generated report: ${report_file}"
