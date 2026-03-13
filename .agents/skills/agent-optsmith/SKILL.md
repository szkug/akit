---
name: agent-optsmith
description: Set up and run the Agent Optsmith workflow for AI coding tasks with measurable impact. Use when users ask to introduce optimization telemetry in a repository, log per-task token and duration metrics, generate weekly error reviews, evaluate skill impact, or compare engineering productivity before and after process improvements.
---

# Agent Optsmith

Use this skill to operationalize and measure continuous optimization in any project.

## Required Paths

- Prefer CLI entrypoint (`optsmith`) so users do not need to resolve install paths manually.
- Use project-local data under `.agents/optsmith-data/`
- CLI entrypoint: `optsmith` (`install`, `update`, `dashboard`, `run`, `metrics`, `optimize`, `version`, `help`).

## Automation Behavior

- Do not ask the user to run logging/report commands manually when this skill is active.
- At task completion, run `optsmith run ...` automatically with task metadata.
- Use `optsmith dashboard` for interactive filtering instead of manual output parsing.
- Use dashboard optimization discovery and run optimize/create actions immediately from the page.

## Mandatory Guarantees

- `agent-optsmith` must persist one task record for every completed task.
- A task should not be reported complete before `optsmith run ...` succeeds.
- Prefer real telemetry values for `total_tokens` and `duration_sec`.
- If runtime telemetry is unavailable, explicitly mark this as telemetry gap and fix the integration.

## Primary Workflow

1. Initialize project data once:
```bash
optsmith install --workspace "$(pwd)"
```

2. Run automation at task completion (collection + analysis + review):
```bash
optsmith run --workspace "$(pwd)" \
  --task-id TASK-1001 \
  --task-type debug \
  --model gpt-5 \
  --used-skill true \
  --skill-name log-analysis-helper \
  --total-tokens 1820 \
  --duration-sec 420 \
  --success true
```

3. Record failures in `.agents/optsmith-data/knowledge-base/errors/` using the generated template:
- `.agents/optsmith-data/templates/error-entry.md`

4. Open the web dashboard for filtering and visualization:
```bash
optsmith dashboard --workspace "$(pwd)" --host 127.0.0.1 --port 8765
```

Use `Skill Optimization Discovery` in the dashboard to optimize existing skills immediately.
Use `New Skill Recommendations` to create-and-optimize candidate new skills immediately.
New or optimized skill files are written under project `.agents/skills/`.

5. Optional direct commands (if you need script-level outputs):
```bash
optsmith metrics --workspace "$(pwd)" --all
optsmith metrics --workspace "$(pwd)" --skill log-analysis-helper
optsmith metrics --workspace "$(pwd)" --all --cutover 2026-03-01
optsmith optimize --workspace "$(pwd)" --skill log-analysis-helper
```

## Interpretation Rules

- Use `token_reduction_pct` to quantify single-skill token savings.
- Use `duration_reduction_pct` to quantify single-skill cycle-time savings.
- Use `delta_avg_tokens_pct`, `delta_avg_duration_pct`, `delta_tasks_per_day_pct` for engineering pre/post impact.
- Require adequate overlap baseline by task type; do not claim gains without no-skill samples on the same task type.

## Decision Policy

- Create or refactor a skill when the same workflow repeats at least three times in seven days.
- Add or update governance rules only when an incident or metric supports the change.
- Keep prompts lean; move repeated deterministic operations to scripts.

## References

- For command snippets, read `references/command-recipes.md`.

## Scripts

- `scripts/setup_loop_workspace.sh`: Initialize project-local data directories.
- `scripts/auto_run_loop.sh`: Auto-run logging + metrics + weekly review in one command.
- `scripts/log_task_run.sh`: Append one standardized task-run record.
- `scripts/weekly_review.sh`: Build weekly optimization report from error KB.
- `scripts/metrics_report.sh`: Compute overall, per-skill, and pre/post metrics.
- `scripts/optimize_skill.sh`: Generate one skill optimization plan with opportunity score.
- `scripts/dashboard_server.sh`: Start local dashboard web server.
- `scripts/dashboard_server.py`: Dashboard backend and UI.
