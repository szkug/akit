#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [[ -f "${SCRIPT_DIR}/../SKILL.md" ]]; then
  mode="skill"
  workspace_dir="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
  workspace_dir="$(cd "${workspace_dir}" && pwd)"
  setup_script="${SCRIPT_DIR}/setup_loop_workspace.sh"
else
  mode="root"
  root_dir="$(cd "${SCRIPT_DIR}/.." && pwd)"
  if [[ "$(basename "${root_dir}")" == ".agent-loop" ]]; then
    workspace_dir="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
  else
    workspace_dir="${OPTSMITH_WORKSPACE_DIR:-${root_dir}}"
  fi
  workspace_dir="$(cd "${workspace_dir}" && pwd)"
  setup_script=""
fi

date_val="$(date +%Y-%m-%d)"
task_id="TASK-$(date +%Y%m%d-%H%M%S)"
task_type="${OPTSMITH_TASK_TYPE:-coding}"
model="${OPTSMITH_MODEL:-gpt-5}"
used_skill="${OPTSMITH_USED_SKILL:-true}"
skill_name="${OPTSMITH_SKILL_NAME:-agent-optsmith}"
total_tokens="${OPTSMITH_TOTAL_TOKENS:-}"
duration_sec="${OPTSMITH_DURATION_SEC:-}"
success="${OPTSMITH_SUCCESS:-true}"
rework_count="${OPTSMITH_REWORK_COUNT:-0}"
cutover="${OPTSMITH_CUTOVER:-}"
run_weekly="true"
enforce_telemetry="false"
tokens_from_session="false"
duration_from_session="false"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/auto_run_loop.sh [options]

Options:
  --task-id <id>
  --task-type <type>
  --model <name>
  --used-skill <true|false>
  --skill-name <name>
  --total-tokens <int>=0
  --duration-sec <int>=0
  --success <true|false>
  --rework-count <int>=0
  --date <YYYY-MM-DD>
  --cutover <YYYY-MM-DD>
  --skip-weekly
  --enforce-telemetry

Description:
  Automatically run the Agent Optsmith workflow:
  1) initialize workspace data in skill mode when missing
  2) log one task run
  3) run metrics reports
  4) run weekly review (unless --skip-weekly)

Telemetry values are resolved in this order:
  - explicit CLI args (--total-tokens / --duration-sec)
  - env overrides:
      tokens: OPTSMITH_TOTAL_TOKENS, CODEX_TOTAL_TOKENS, OPENAI_TOTAL_TOKENS, TASK_TOTAL_TOKENS
      duration: OPTSMITH_DURATION_SEC, CODEX_TASK_DURATION_SEC, TASK_DURATION_SEC
  - fallback duration from task start timestamp:
      OPTSMITH_TASK_START_TS or TASK_START_TS (unix epoch seconds)
  - fallback from local Codex session logs:
      CODEX_HOME/sessions and CODEX_HOME/archived_sessions
      (thread match uses CODEX_THREAD_ID when available)
  - final fallback: 0
EOF
}

first_non_empty() {
  for value in "$@"; do
    if [[ -n "${value}" ]]; then
      printf '%s' "${value}"
      return 0
    fi
  done
  return 1
}

resolve_duration_from_start_ts() {
  local start_ts="$1"
  if [[ ! "${start_ts}" =~ ^[0-9]+$ ]]; then
    return 1
  fi
  local now_ts
  now_ts="$(date +%s)"
  if [[ "${now_ts}" -lt "${start_ts}" ]]; then
    return 1
  fi
  printf '%s' "$((now_ts - start_ts))"
}

resolve_codex_session_file() {
  local codex_home="${CODEX_HOME:-$HOME/.codex}"
  local session_override
  session_override="$(first_non_empty \
    "${OPTSMITH_CODEX_SESSION_FILE:-}" \
    "${CODEX_SESSION_FILE:-}" || true)"
  if [[ -n "${session_override}" && -f "${session_override}" ]]; then
    printf '%s' "${session_override}"
    return 0
  fi

  local thread_id
  thread_id="$(first_non_empty \
    "${OPTSMITH_CODEX_THREAD_ID:-}" \
    "${CODEX_THREAD_ID:-}" || true)"

  local candidate_dirs=(
    "${codex_home}/sessions"
    "${codex_home}/archived_sessions"
  )
  local candidate=""
  local dir

  if [[ -n "${thread_id}" ]]; then
    for dir in "${candidate_dirs[@]}"; do
      if [[ -d "${dir}" ]]; then
        candidate="$(find "${dir}" -type f -name "rollout-*-${thread_id}.jsonl" 2>/dev/null | sort | tail -n 1 || true)"
        if [[ -n "${candidate}" ]]; then
          printf '%s' "${candidate}"
          return 0
        fi
      fi
    done
  fi

  for dir in "${candidate_dirs[@]}"; do
    if [[ -d "${dir}" ]]; then
      candidate="$(find "${dir}" -type f -name "rollout-*.jsonl" 2>/dev/null | sort | tail -n 1 || true)"
      if [[ -n "${candidate}" ]]; then
        printf '%s' "${candidate}"
        return 0
      fi
    fi
  done

  return 1
}

resolve_telemetry_from_codex_session() {
  local session_file="$1"
  if [[ ! -f "${session_file}" ]]; then
    return 1
  fi
  if ! command -v python3 >/dev/null 2>&1; then
    return 1
  fi

  python3 - "${session_file}" <<'PY'
import datetime as dt
import json
import sys


def parse_ts(raw):
    if not isinstance(raw, str) or not raw:
        return None
    normalized = raw
    if normalized.endswith("Z"):
        normalized = normalized[:-1] + "+00:00"
    try:
        return dt.datetime.fromisoformat(normalized).timestamp()
    except ValueError:
        return None


session_path = sys.argv[1]
session_start_ts = None
turn_start_ts = None
last_event_ts = None
turn_token_sum = 0
turn_token_count = 0
last_token_value = 0

with open(session_path, "r", encoding="utf-8") as handle:
    for raw_line in handle:
        line = raw_line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue

        event_ts = parse_ts(obj.get("timestamp"))
        if event_ts is not None:
            last_event_ts = event_ts

        entry_type = obj.get("type")
        payload = obj.get("payload")

        if entry_type == "session_meta":
            session_start_ts = event_ts
            continue

        if entry_type == "turn_context":
            turn_start_ts = event_ts
            turn_token_sum = 0
            turn_token_count = 0
            continue

        if entry_type != "event_msg" or not isinstance(payload, dict):
            continue
        if payload.get("type") != "token_count":
            continue

        info = payload.get("info") or {}
        last_usage = info.get("last_token_usage") or {}
        token_value = last_usage.get("total_tokens")
        if isinstance(token_value, (int, float)):
            token_int = int(token_value)
            if token_int < 0:
                token_int = 0
            last_token_value = token_int
            if turn_start_ts is not None:
                turn_token_sum += token_int
                turn_token_count += 1

tokens = turn_token_sum if turn_token_count > 0 else last_token_value
if tokens < 0:
    tokens = 0

duration = 0
start_ts = turn_start_ts if turn_start_ts is not None else session_start_ts
if start_ts is not None and last_event_ts is not None and last_event_ts >= start_ts:
    duration = int(last_event_ts - start_ts)
if duration < 0:
    duration = 0

if duration == 0 and tokens > 0:
    duration = 1

print(f"{tokens} {duration}")
PY
}

while [[ $# -gt 0 ]]; do
  case "$1" in
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
    --date) date_val="${2:-}"; shift 2 ;;
    --cutover) cutover="${2:-}"; shift 2 ;;
    --skip-weekly) run_weekly="false"; shift ;;
    --enforce-telemetry) enforce_telemetry="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ "${mode}" == "skill" && ! -d "${workspace_dir}/.agents/optsmith-data" ]]; then
  "${setup_script}" --workspace "${workspace_dir}" >/dev/null
fi

if [[ -z "${task_id}" ]]; then
  echo "error: --task-id must not be empty"
  exit 1
fi

if [[ "${used_skill}" == "false" ]]; then
  skill_name=""
fi

if [[ -z "${total_tokens}" ]]; then
  total_tokens="$(first_non_empty \
    "${OPTSMITH_TOTAL_TOKENS:-}" \
    "${CODEX_TOTAL_TOKENS:-}" \
    "${OPENAI_TOTAL_TOKENS:-}" \
    "${TASK_TOTAL_TOKENS:-}" || true)"
fi

if [[ -z "${duration_sec}" ]]; then
  duration_sec="$(first_non_empty \
    "${OPTSMITH_DURATION_SEC:-}" \
    "${CODEX_TASK_DURATION_SEC:-}" \
    "${TASK_DURATION_SEC:-}" || true)"
fi

if [[ -z "${duration_sec}" ]]; then
  duration_sec="$(resolve_duration_from_start_ts "$(first_non_empty \
    "${OPTSMITH_TASK_START_TS:-}" \
    "${TASK_START_TS:-}" || true)" || true)"
fi

if [[ -z "${total_tokens}" || -z "${duration_sec}" ]]; then
  session_file="$(resolve_codex_session_file || true)"
  if [[ -n "${session_file}" ]]; then
    session_telemetry="$(resolve_telemetry_from_codex_session "${session_file}" || true)"
    if [[ -n "${session_telemetry}" ]]; then
      read -r session_tokens session_duration <<<"${session_telemetry}"
      if [[ -z "${total_tokens}" && "${session_tokens:-}" =~ ^[0-9]+$ ]]; then
        total_tokens="${session_tokens}"
        tokens_from_session="true"
      fi
      if [[ -z "${duration_sec}" && "${session_duration:-}" =~ ^[0-9]+$ ]]; then
        duration_sec="${session_duration}"
        duration_from_session="true"
      fi
    fi
  fi
fi

if [[ -z "${total_tokens}" ]]; then
  total_tokens="0"
fi
if [[ -z "${duration_sec}" ]]; then
  duration_sec="0"
fi

if [[ ! "${total_tokens}" =~ ^[0-9]+$ ]]; then
  echo "error: resolved total_tokens is not an integer: ${total_tokens}"
  exit 1
fi
if [[ ! "${duration_sec}" =~ ^[0-9]+$ ]]; then
  echo "error: resolved duration_sec is not an integer: ${duration_sec}"
  exit 1
fi

if [[ "${enforce_telemetry}" == "true" && ( "${total_tokens}" == "0" || "${duration_sec}" == "0" ) ]]; then
  echo "error: telemetry missing while --enforce-telemetry is enabled"
  echo "hint: pass --total-tokens/--duration-sec or set telemetry env vars"
  exit 1
fi

if [[ "${total_tokens}" == "0" || "${duration_sec}" == "0" ]]; then
  echo "warning: telemetry incomplete (total_tokens=${total_tokens}, duration_sec=${duration_sec})"
fi

if [[ "${tokens_from_session}" == "true" || "${duration_from_session}" == "true" ]]; then
  echo "info: telemetry resolved from Codex session log"
fi

metrics_args=(--all)
if [[ -n "${cutover}" ]]; then
  metrics_args+=(--cutover "${cutover}")
fi

skill_metrics_args=(--skill "${skill_name}")
if [[ -n "${cutover}" ]]; then
  skill_metrics_args+=(--cutover "${cutover}")
fi

echo "[1/4] logging task run"
"${SCRIPT_DIR}/log_task_run.sh" \
  --date "${date_val}" \
  --task-id "${task_id}" \
  --task-type "${task_type}" \
  --model "${model}" \
  --used-skill "${used_skill}" \
  --skill-name "${skill_name}" \
  --total-tokens "${total_tokens}" \
  --duration-sec "${duration_sec}" \
  --success "${success}" \
  --rework-count "${rework_count}"

echo "[2/4] running overall metrics"
"${SCRIPT_DIR}/metrics_report.sh" "${metrics_args[@]}"

echo "[3/4] running skill metrics"
if [[ "${used_skill}" == "true" && -n "${skill_name}" ]]; then
  "${SCRIPT_DIR}/metrics_report.sh" "${skill_metrics_args[@]}"
else
  echo "skipped: no skill row for this run"
fi

echo "[4/4] running weekly review"
if [[ "${run_weekly}" == "true" ]]; then
  "${SCRIPT_DIR}/weekly_review.sh"
else
  echo "skipped: --skip-weekly"
fi

echo "auto workflow run completed"
