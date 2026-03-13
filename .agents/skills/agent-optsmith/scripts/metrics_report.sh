#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKSPACE_DIR="${OPTSMITH_WORKSPACE_DIR:-$(pwd)}"
WORKSPACE_DIR="$(cd "${WORKSPACE_DIR}" && pwd)"
DATA_FILE_DEFAULT="${WORKSPACE_DIR}/.agents/optsmith-data/metrics/task-runs.csv"
DATA_FILE="${OPTSMITH_DATA_FILE:-${DATA_FILE_DEFAULT}}"
mode=""
skill_name=""
cutover=""

usage() {
  cat <<'EOF'
Usage:
  OPTSMITH_DATA_FILE=.agents/optsmith-data/metrics/task-runs.csv ./scripts/metrics_report.sh --all

  ./scripts/metrics_report.sh --all [--cutover YYYY-MM-DD]
  ./scripts/metrics_report.sh --skill <skill-name> [--cutover YYYY-MM-DD]
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all)
      mode="all"
      shift
      ;;
    --skill)
      mode="skill"
      skill_name="${2:-}"
      shift 2
      ;;
    --cutover)
      cutover="${2:-}"
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

if [[ -z "${mode}" ]]; then
  usage
  exit 1
fi

if [[ ! -f "${DATA_FILE}" ]]; then
  echo "error: missing data file: ${DATA_FILE}"
  exit 1
fi

if [[ "${mode}" == "skill" && -z "${skill_name}" ]]; then
  echo "error: --skill requires a non-empty skill name"
  exit 1
fi

awk -F',' -v mode="${mode}" -v target_skill="${skill_name}" -v cutover="${cutover}" -v data_file="${DATA_FILE}" '
BEGIN {
  used_skill_idx = 5
  skill_idx = 6
  tokens_idx = 7
  duration_idx = 8
  success_idx = 9
  rework_idx = 10
}

function pct(a, b) {
  if (b == 0) return "n/a"
  return sprintf("%.2f%%", (a / b) * 100)
}

function safe_avg(sum, count) {
  if (count == 0) return "n/a"
  return sprintf("%.2f", sum / count)
}

function print_skill_block(skill,   t, overlap_count, s_tokens, s_cnt, s_dur, s_success, s_rework, b_tokens, b_cnt, b_dur, b_success, b_rework, token_reduction, dur_reduction, success_delta, rework_delta, skill_success_rate, base_success_rate, skill_rework_rate, base_rework_rate) {
  overlap_count = 0
  s_tokens = s_cnt = s_dur = s_success = s_rework = 0
  b_tokens = b_cnt = b_dur = b_success = b_rework = 0

  for (t in task_types_seen) {
    if (skill_count[skill, t] > 0 && base_count[t] > 0) {
      overlap_count++
      s_tokens += skill_tokens[skill, t]
      s_cnt += skill_count[skill, t]
      s_dur += skill_duration[skill, t]
      s_success += skill_success[skill, t]
      s_rework += skill_rework[skill, t]

      b_tokens += base_tokens[t]
      b_cnt += base_count[t]
      b_dur += base_duration[t]
      b_success += base_success[t]
      b_rework += base_rework[t]
    }
  }

  print ""
  print "Skill: " skill
  print "  overlap_task_types: " overlap_count

  if (overlap_count == 0 || s_cnt == 0 || b_cnt == 0) {
    print "  status: insufficient baseline (need no-skill samples on same task_type)"
    return
  }

  token_reduction = ((b_tokens / b_cnt) - (s_tokens / s_cnt))
  dur_reduction = ((b_dur / b_cnt) - (s_dur / s_cnt))
  skill_success_rate = (s_success / s_cnt)
  base_success_rate = (b_success / b_cnt)
  success_delta = (skill_success_rate - base_success_rate) * 100
  skill_rework_rate = (s_rework / s_cnt)
  base_rework_rate = (b_rework / b_cnt)
  rework_delta = (skill_rework_rate - base_rework_rate)

  print "  sample_size_skill: " s_cnt
  print "  sample_size_baseline: " b_cnt
  print "  skill_avg_tokens: " sprintf("%.2f", s_tokens / s_cnt)
  print "  baseline_avg_tokens: " sprintf("%.2f", b_tokens / b_cnt)
  print "  token_reduction_pct: " pct(token_reduction, (b_tokens / b_cnt))
  print "  skill_avg_duration_sec: " sprintf("%.2f", s_dur / s_cnt)
  print "  baseline_avg_duration_sec: " sprintf("%.2f", b_dur / b_cnt)
  print "  duration_reduction_pct: " pct(dur_reduction, (b_dur / b_cnt))
  print "  success_rate_delta_pp: " sprintf("%.2fpp", success_delta)
  print "  rework_rate_delta: " sprintf("%.3f", rework_delta)
}

NR == 1 {
  if ($4 == "project") {
    used_skill_idx = 6
    skill_idx = 7
    tokens_idx = 8
    duration_idx = 9
    success_idx = 10
    rework_idx = 11
  }
  next
} # header
/^#/ { next }    # comment rows
NF < rework_idx { next }
{
  date = $1
  task_type = $3
  used_skill = tolower($used_skill_idx)
  skill = $skill_idx
  tokens = $tokens_idx + 0
  duration = $duration_idx + 0
  success = tolower($success_idx)
  rework = $rework_idx + 0

  total_tasks++
  total_tokens += tokens
  total_duration += duration
  total_rework += rework
  if (success == "true") total_success++

  if (!(date in seen_days)) {
    seen_days[date] = 1
    active_days++
  }

  if (cutover != "") {
    if (date < cutover) {
      pre_tasks++
      pre_tokens += tokens
      pre_duration += duration
      pre_rework += rework
      if (success == "true") pre_success++
      if (!(date in pre_days)) {
        pre_days[date] = 1
        pre_active_days++
      }
    } else {
      post_tasks++
      post_tokens += tokens
      post_duration += duration
      post_rework += rework
      if (success == "true") post_success++
      if (!(date in post_days)) {
        post_days[date] = 1
        post_active_days++
      }
    }
  }

  task_types_seen[task_type] = 1

  if (used_skill == "true" && skill != "") {
    skills_seen[skill] = 1
    skill_tokens[skill, task_type] += tokens
    skill_duration[skill, task_type] += duration
    skill_count[skill, task_type]++
    if (success == "true") skill_success[skill, task_type]++
    skill_rework[skill, task_type] += rework
  } else {
    base_tokens[task_type] += tokens
    base_duration[task_type] += duration
    base_count[task_type]++
    if (success == "true") base_success[task_type]++
    base_rework[task_type] += rework
  }
}
END {
  if (total_tasks == 0) {
    print "No task rows found in " data_file
    exit 0
  }

  print "Overall Metrics"
  print "  tasks: " total_tasks
  print "  active_days: " active_days
  print "  tasks_per_day: " safe_avg(total_tasks, active_days)
  print "  avg_tokens: " safe_avg(total_tokens, total_tasks)
  print "  avg_duration_sec: " safe_avg(total_duration, total_tasks)
  print "  success_rate: " pct(total_success, total_tasks)
  print "  rework_rate: " safe_avg(total_rework, total_tasks)

  if (cutover != "") {
    print ""
    print "Pre/Post Metrics (cutover=" cutover ")"
    print "  pre_tasks: " (pre_tasks + 0)
    print "  pre_tasks_per_day: " safe_avg(pre_tasks + 0, pre_active_days + 0)
    print "  pre_avg_tokens: " safe_avg(pre_tokens + 0, pre_tasks + 0)
    print "  pre_avg_duration_sec: " safe_avg(pre_duration + 0, pre_tasks + 0)
    print "  pre_success_rate: " pct(pre_success + 0, pre_tasks + 0)
    print "  pre_rework_rate: " safe_avg(pre_rework + 0, pre_tasks + 0)
    print "  post_tasks: " (post_tasks + 0)
    print "  post_tasks_per_day: " safe_avg(post_tasks + 0, post_active_days + 0)
    print "  post_avg_tokens: " safe_avg(post_tokens + 0, post_tasks + 0)
    print "  post_avg_duration_sec: " safe_avg(post_duration + 0, post_tasks + 0)
    print "  post_success_rate: " pct(post_success + 0, post_tasks + 0)
    print "  post_rework_rate: " safe_avg(post_rework + 0, post_tasks + 0)

    if ((pre_tasks + 0) > 0 && (post_tasks + 0) > 0 && (pre_active_days + 0) > 0 && (post_active_days + 0) > 0) {
      print "  delta_avg_tokens_pct: " pct((post_tokens / post_tasks) - (pre_tokens / pre_tasks), (pre_tokens / pre_tasks))
      print "  delta_avg_duration_pct: " pct((post_duration / post_tasks) - (pre_duration / pre_tasks), (pre_duration / pre_tasks))
      print "  delta_success_rate_pp: " sprintf("%.2fpp", ((post_success / post_tasks) - (pre_success / pre_tasks)) * 100)
      print "  delta_tasks_per_day_pct: " pct((post_tasks / post_active_days) - (pre_tasks / pre_active_days), (pre_tasks / pre_active_days))
    }
  }

  if (mode == "skill") {
    if (!(target_skill in skills_seen)) {
      print ""
      print "Skill: " target_skill
      print "  status: no rows found"
      exit 0
    }
    print_skill_block(target_skill)
    exit 0
  }

  if (mode == "all") {
    print ""
    print "Skill Comparison (vs no-skill baseline by task_type)"
    any_skill = 0
    for (s in skills_seen) {
      any_skill = 1
      print_skill_block(s)
    }
    if (!any_skill) {
      print "No skill rows found."
    }
  }
}
' "${DATA_FILE}"
