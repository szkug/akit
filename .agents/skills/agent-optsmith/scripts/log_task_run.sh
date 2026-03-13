#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKSPACE_DIR="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
WORKSPACE_DIR="$(cd "${WORKSPACE_DIR}" && pwd)"
DATA_FILE_DEFAULT="${WORKSPACE_DIR}/.agents/optsmith-data/metrics/task-runs.csv"
DATA_FILE="${OPTSMITH_DATA_FILE:-${DATA_FILE_DEFAULT}}"

date_val="$(date +%Y-%m-%d)"
task_id=""
task_type=""
model=""
used_skill=""
skill_name=""
total_tokens=""
duration_sec=""
success=""
rework_count="0"

usage() {
  cat <<'EOF'
Usage:
  OPTSMITH_DATA_FILE=.agents/optsmith-data/metrics/task-runs.csv ./scripts/log_task_run.sh ...

  ./scripts/log_task_run.sh \
    --task-id TASK-1001 \
    --task-type debug \
    --model gpt-5 \
    --used-skill true \
    --skill-name log-analysis-helper \
    --total-tokens 1820 \
    --duration-sec 420 \
    --success true \
    [--rework-count 0] \
    [--date YYYY-MM-DD]
EOF
}

require_no_comma() {
  local value="$1"
  local field="$2"
  if [[ "$value" == *,* ]]; then
    echo "error: ${field} must not contain comma"
    exit 1
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date) date_val="${2:-}"; shift 2 ;;
    --task-id) task_id="${2:-}"; shift 2 ;;
    --task-type) task_type="${2:-}"; shift 2 ;;
    --project) shift 2 ;; # deprecated no-op
    --model) model="${2:-}"; shift 2 ;;
    --used-skill) used_skill="${2:-}"; shift 2 ;;
    --skill-name) skill_name="${2:-}"; shift 2 ;;
    --total-tokens) total_tokens="${2:-}"; shift 2 ;;
    --duration-sec) duration_sec="${2:-}"; shift 2 ;;
    --success) success="${2:-}"; shift 2 ;;
    --rework-count) rework_count="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

for pair in \
  "$task_id task_id" \
  "$task_type task_type" \
  "$model model" \
  "$used_skill used_skill" \
  "$total_tokens total_tokens" \
  "$duration_sec duration_sec" \
  "$success success"; do
  value="${pair%% *}"
  field="${pair##* }"
  if [[ -z "$value" ]]; then
    echo "error: missing required field: ${field}"
    usage
    exit 1
  fi
done

if [[ "$used_skill" != "true" && "$used_skill" != "false" ]]; then
  echo "error: --used-skill must be true or false"
  exit 1
fi

if [[ "$success" != "true" && "$success" != "false" ]]; then
  echo "error: --success must be true or false"
  exit 1
fi

if [[ "$used_skill" == "false" ]]; then
  skill_name=""
fi

if [[ ! "$total_tokens" =~ ^[0-9]+$ ]]; then
  echo "error: --total-tokens must be an integer >= 0"
  exit 1
fi

if [[ ! "$duration_sec" =~ ^[0-9]+$ ]]; then
  echo "error: --duration-sec must be an integer >= 0"
  exit 1
fi

if [[ ! "$rework_count" =~ ^[0-9]+$ ]]; then
  echo "error: --rework-count must be an integer >= 0"
  exit 1
fi

for pair in \
  "$date_val date" \
  "$task_id task_id" \
  "$task_type task_type" \
  "$model model" \
  "$used_skill used_skill" \
  "$skill_name skill_name" \
  "$success success"; do
  value="${pair%% *}"
  field="${pair##* }"
  require_no_comma "$value" "$field"
done

if [[ ! -f "$DATA_FILE" ]]; then
  mkdir -p "$(dirname "$DATA_FILE")"
  echo "date,task_id,task_type,model,used_skill,skill_name,total_tokens,duration_sec,success,rework_count" > "$DATA_FILE"
else
  current_header="$(head -n 1 "$DATA_FILE")"
  legacy_header="date,task_id,task_type,project,model,used_skill,skill_name,total_tokens,duration_sec,success,rework_count"
  new_header="date,task_id,task_type,model,used_skill,skill_name,total_tokens,duration_sec,success,rework_count"
  if [[ "${current_header}" == "${legacy_header}" ]]; then
    tmp_file="$(mktemp)"
    awk -F',' 'BEGIN { OFS="," }
NR == 1 { print "date,task_id,task_type,model,used_skill,skill_name,total_tokens,duration_sec,success,rework_count"; next }
NF >= 11 { print $1,$2,$3,$5,$6,$7,$8,$9,$10,$11; next }
NF == 10 { print $1,$2,$3,$4,$5,$6,$7,$8,$9,$10 }
' "${DATA_FILE}" > "${tmp_file}"
    mv "${tmp_file}" "${DATA_FILE}"
    echo "info: migrated legacy task-runs.csv header (removed project column)"
  elif [[ "${current_header}" != "${new_header}" ]]; then
    echo "error: unsupported task-runs.csv header: ${current_header}"
    exit 1
  fi
fi

echo "${date_val},${task_id},${task_type},${model},${used_skill},${skill_name},${total_tokens},${duration_sec},${success},${rework_count}" >> "${DATA_FILE}"
echo "logged: task_id=${task_id} used_skill=${used_skill} skill=${skill_name:-none}"
