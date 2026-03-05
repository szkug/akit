# AGENTS.md

## Mission

Use project-local skills and scripts to deliver high-quality outcomes with measurable engineering impact.

## Skill Source

Primary optimization skill is vendored via submodule:

- path: `.agent-loop/skills/agent-self-optimizing-loop/SKILL.md`

## Trigger Rules

Always load and use `agent-self-optimizing-loop` when any request matches one or more of these intents:

1. Introduce or operate self-optimization workflow in this repository.
2. Log task token/duration/success/rework metrics.
3. Compare skill impact vs baseline (`token_reduction_pct`, `duration_reduction_pct`, success/rework deltas).
4. Run weekly incident/error review.
5. Analyze pre/post efficiency around a cutover date.
6. Discover optimization opportunities for existing skills.
7. Open or use the local dashboard for metric filtering and optimization triggering.

## Required Workflow When Triggered

1. Ensure workspace data is initialized:

```bash
./.agent-loop/scripts/setup_loop_workspace.sh --workspace "$(pwd)"
```

2. At task completion, auto-run collection + analysis + weekly review:

```bash
./.agent-loop/scripts/auto_run_loop.sh \
  --task-id <task-id> \
  --task-type <task-type> \
  --project akit \
  --model <model> \
  --used-skill true \
  --skill-name agent-self-optimizing-loop \
  --total-tokens <tokens> \
  --duration-sec <duration-sec> \
  --success <true|false> \
  --rework-count <count>
```

3. For interactive analysis, use dashboard:

```bash
./.agent-loop/scripts/dashboard_server.sh --host 127.0.0.1 --port 8765
```

4. For per-skill optimization plan generation:

```bash
./.agent-loop/scripts/optimize_skill.sh --skill <skill-name>
```

## Definition of Done

A task is considered complete only when all applicable checks pass:

1. Requested code/doc change is complete.
2. If optimization skill was triggered, at least one new row is added to `.agent-loop-data/metrics/task-runs.csv`.
3. For analysis/optimization requests, expected report artifacts are generated under `.agent-loop-data/reports/`.
