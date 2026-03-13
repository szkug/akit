#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [[ -f "${SCRIPT_DIR}/../SKILL.md" ]]; then
  mode="skill"
  workspace_dir="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
  workspace_dir="$(cd "${workspace_dir}" && pwd)"
  data_file_default="${workspace_dir}/.agents/optsmith-data/metrics/task-runs.csv"
  kb_dir_default="${workspace_dir}/.agents/optsmith-data/knowledge-base/errors"
  report_dir_default="${workspace_dir}/.agents/optsmith-data/reports/skill-optimization"
else
  mode="root"
  workspace_dir="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
  workspace_dir="$(cd "${workspace_dir}" && pwd)"
  data_file_default="${workspace_dir}/.agents/optsmith-data/metrics/task-runs.csv"
  kb_dir_default="${workspace_dir}/.agents/optsmith-data/knowledge-base/errors"
  report_dir_default="${workspace_dir}/.agents/optsmith-data/reports/skill-optimization"
fi

data_file="${OPTSMITH_DATA_FILE:-${data_file_default}}"
kb_dir="${OPTSMITH_KB_DIR:-${kb_dir_default}}"
report_dir="${OPTSMITH_OPT_REPORT_DIR:-${report_dir_default}}"

skill_name=""
start_date=""
end_date=""
cutover=""

usage() {
  cat <<'EOF'
Usage:
  ./scripts/optimize_skill.sh --skill <skill-name> [options]

Options:
  --skill <name>         Required skill name to optimize.
  --start <YYYY-MM-DD>   Optional start date filter.
  --end <YYYY-MM-DD>     Optional end date filter.
  --cutover <YYYY-MM-DD> Optional cutover date for pre/post metrics.

Description:
  Analyze one skill, discover optimization opportunities, and generate
  a markdown optimization plan report under .agents/optsmith-data/reports/skill-optimization/.
EOF
}

is_date() {
  [[ "${1:-}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) skill_name="${2:-}"; shift 2 ;;
    --start) start_date="${2:-}"; shift 2 ;;
    --end) end_date="${2:-}"; shift 2 ;;
    --cutover) cutover="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${skill_name}" ]]; then
  echo "error: --skill is required"
  usage
  exit 1
fi

if [[ -n "${start_date}" ]] && ! is_date "${start_date}"; then
  echo "error: invalid --start date format (expected YYYY-MM-DD)"
  exit 1
fi

if [[ -n "${end_date}" ]] && ! is_date "${end_date}"; then
  echo "error: invalid --end date format (expected YYYY-MM-DD)"
  exit 1
fi

if [[ -n "${cutover}" ]] && ! is_date "${cutover}"; then
  echo "error: invalid --cutover date format (expected YYYY-MM-DD)"
  exit 1
fi

if [[ -n "${start_date}" && -n "${end_date}" && "${start_date}" > "${end_date}" ]]; then
  echo "error: --start must be <= --end"
  exit 1
fi

if [[ ! -f "${data_file}" ]]; then
  echo "error: data file not found: ${data_file}"
  exit 1
fi

mkdir -p "${report_dir}"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

filtered_data="${tmp_dir}/filtered.csv"
awk -F',' -v start="${start_date}" -v end="${end_date}" '
NR == 1 { print; next }
{
  d=$1
  if ((start == "" || d >= start) && (end == "" || d <= end)) {
    print
  }
}
' "${data_file}" > "${filtered_data}"

metrics_cmd=("${SCRIPT_DIR}/metrics_report.sh" --skill "${skill_name}")
if [[ -n "${cutover}" ]]; then
  metrics_cmd+=(--cutover "${cutover}")
fi

metrics_output="$(OPTSMITH_DATA_FILE="${filtered_data}" "${metrics_cmd[@]}")"

extract_metric() {
  local key="$1"
  printf '%s\n' "${metrics_output}" | awk -F': ' -v k="$key" '
  $1 == "  "k { print $2; exit }
  '
}

status_text="$(extract_metric "status")"
token_reduction="$(extract_metric "token_reduction_pct")"
duration_reduction="$(extract_metric "duration_reduction_pct")"
success_delta="$(extract_metric "success_rate_delta_pp")"
rework_delta="$(extract_metric "rework_rate_delta")"
sample_skill="$(extract_metric "sample_size_skill")"
sample_baseline="$(extract_metric "sample_size_baseline")"
overlap_task_types="$(extract_metric "overlap_task_types")"

parse_percent_num() {
  local value="${1:-}"
  value="${value%\%}"
  if [[ "${value}" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
    printf '%s' "${value}"
  fi
}

parse_pp_num() {
  local value="${1:-}"
  value="${value%pp}"
  if [[ "${value}" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
    printf '%s' "${value}"
  fi
}

parse_float_num() {
  local value="${1:-}"
  if [[ "${value}" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
    printf '%s' "${value}"
  fi
}

token_num="$(parse_percent_num "${token_reduction}")"
duration_num="$(parse_percent_num "${duration_reduction}")"
success_num="$(parse_pp_num "${success_delta}")"
rework_num="$(parse_float_num "${rework_delta}")"

opportunity_score=0
optimization_status="healthy"
reasons_tmp="${tmp_dir}/reasons.txt"
actions_tmp="${tmp_dir}/actions.txt"
root_causes_tmp="${tmp_dir}/root-causes.txt"
touch "${reasons_tmp}" "${actions_tmp}" "${root_causes_tmp}"

if [[ "${status_text}" == *"insufficient baseline"* ]]; then
  opportunity_score=$((opportunity_score + 50))
  echo "Insufficient baseline on matching task types." >> "${reasons_tmp}"
  echo "Collect at least 10 no-skill baseline samples for the same task types." >> "${actions_tmp}"
fi

if [[ -n "${token_num}" ]] && awk "BEGIN { exit !(${token_num} < 0) }"; then
  opportunity_score=$((opportunity_score + 35))
  echo "Token cost is higher than baseline (${token_reduction})." >> "${reasons_tmp}"
  echo "Tighten trigger conditions and trim prompt context to reduce token overhead." >> "${actions_tmp}"
fi

if [[ -n "${duration_num}" ]] && awk "BEGIN { exit !(${duration_num} < 0) }"; then
  opportunity_score=$((opportunity_score + 20))
  echo "Execution duration is slower than baseline (${duration_reduction})." >> "${reasons_tmp}"
  echo "Move repeated steps into scripts and reduce serial operations." >> "${actions_tmp}"
fi

if [[ -n "${success_num}" ]] && awk "BEGIN { exit !(${success_num} < 0) }"; then
  opportunity_score=$((opportunity_score + 35))
  echo "Success rate regressed (${success_delta})." >> "${reasons_tmp}"
  echo "Add mandatory verification gates and stronger done criteria in SKILL.md." >> "${actions_tmp}"
fi

if [[ -n "${rework_num}" ]] && awk "BEGIN { exit !(${rework_num} > 0) }"; then
  opportunity_score=$((opportunity_score + 30))
  echo "Rework rate increased (${rework_delta})." >> "${reasons_tmp}"
  echo "Add prevention checks and rollback-safe checkpoints for this skill." >> "${actions_tmp}"
fi

if [[ -n "${sample_skill}" && "${sample_skill}" =~ ^[0-9]+$ && "${sample_skill}" -lt 10 ]]; then
  opportunity_score=$((opportunity_score + 10))
  echo "Skill sample size is small (${sample_skill}), confidence is low." >> "${reasons_tmp}"
  echo "Increase samples before finalizing optimization decisions." >> "${actions_tmp}"
fi

task_types_tmp="${tmp_dir}/task-types.txt"
awk -F',' -v s="${skill_name}" '
BEGIN {
  used_skill_idx = 5
  skill_idx = 6
}
NR == 1 {
  if ($4 == "project") {
    used_skill_idx = 6
    skill_idx = 7
  }
  next
}
tolower($used_skill_idx) == "true" && $skill_idx == s {
  print $3
}
' "${filtered_data}" | sort -u > "${task_types_tmp}"

if [[ -d "${kb_dir}" && -s "${task_types_tmp}" ]]; then
  for file in "${kb_dir}"/*.md; do
    [[ -e "${file}" ]] || continue
    entry_date="$(awk -F': ' '/^date: /{print $2; exit}' "${file}")"
    task_type="$(awk -F': ' '/^task_type: /{print $2; exit}' "${file}")"
    root_cause="$(awk -F': ' '/^root_cause: /{print $2; exit}' "${file}")"
    [[ -n "${entry_date}" ]] || continue
    [[ -n "${task_type}" ]] || continue
    [[ -n "${root_cause}" ]] || continue
    if [[ -n "${start_date}" && "${entry_date}" < "${start_date}" ]]; then
      continue
    fi
    if [[ -n "${end_date}" && "${entry_date}" > "${end_date}" ]]; then
      continue
    fi
    if grep -Fxq "${task_type}" "${task_types_tmp}"; then
      echo "${root_cause}" >> "${root_causes_tmp}"
    fi
  done
fi

top_root_causes="$(sort "${root_causes_tmp}" | uniq -c | sort -nr | head -n 3 | sed -E 's/^[[:space:]]*([0-9]+)[[:space:]]+/- \1x /')"
if [[ -z "${top_root_causes}" ]]; then
  top_root_causes="- none"
fi

if [[ "${opportunity_score}" -ge 70 ]]; then
  optimization_status="needs_optimization"
elif [[ "${opportunity_score}" -ge 35 ]]; then
  optimization_status="watch"
else
  optimization_status="healthy"
fi

if [[ ! -s "${actions_tmp}" ]]; then
  cat >> "${actions_tmp}" <<'EOF'
Keep current workflow and continue monitoring weekly metrics.
EOF
fi

if [[ ! -s "${reasons_tmp}" ]]; then
  cat >> "${reasons_tmp}" <<'EOF'
No major regression signal was detected in the selected range.
EOF
fi

safe_skill_name="$(printf '%s' "${skill_name}" | tr '/ ' '--' | tr -cd '[:alnum:]._-')"
today="$(date +%Y-%m-%d)"
report_file="${report_dir}/${today}-${safe_skill_name}-optimization-plan.md"

cat > "${report_file}" <<EOF
# Skill Optimization Plan

## Scope

- skill: ${skill_name}
- mode: ${mode}
- data_file: ${data_file}
- kb_dir: ${kb_dir}
- date_range_start: ${start_date:-all}
- date_range_end: ${end_date:-all}
- cutover: ${cutover:-none}

## Opportunity Assessment

- optimization_status: ${optimization_status}
- opportunity_score: ${opportunity_score}
- overlap_task_types: ${overlap_task_types:-n/a}
- sample_size_skill: ${sample_skill:-n/a}
- sample_size_baseline: ${sample_baseline:-n/a}
- token_reduction_pct: ${token_reduction:-n/a}
- duration_reduction_pct: ${duration_reduction:-n/a}
- success_rate_delta_pp: ${success_delta:-n/a}
- rework_rate_delta: ${rework_delta:-n/a}

## Key Findings

$(sed -E 's/^/- /' "${reasons_tmp}")

## Suggested Optimization Actions

$(nl -ba "${actions_tmp}" | sed -E 's/^[[:space:]]*([0-9]+)[[:space:]]+/\1. /')

## Related Root Causes (From Error KB)

${top_root_causes}

## Trigger Command

\`\`\`bash
${SCRIPT_DIR}/optimize_skill.sh --skill "${skill_name}" --start "${start_date:-YYYY-MM-DD}" --end "${end_date:-YYYY-MM-DD}"${cutover:+ --cutover "${cutover}"}
\`\`\`

## Raw Skill Metrics Output

\`\`\`text
${metrics_output}
\`\`\`
EOF

echo "generated optimization report: ${report_file}"
echo "optimization_status: ${optimization_status}"
echo "opportunity_score: ${opportunity_score}"
