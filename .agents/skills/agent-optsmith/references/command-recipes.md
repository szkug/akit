# Command Recipes

## 0) CLI entrypoint (no submodule)

```bash
optsmith update
optsmith install --workspace "$(pwd)"
optsmith dashboard --workspace "$(pwd)" --host 127.0.0.1 --port 8765
optsmith run --workspace "$(pwd)" --task-id TASK-1001 --task-type debug --model gpt-5 --used-skill true --skill-name log-analysis-helper --success true
optsmith metrics --workspace "$(pwd)" --all
optsmith optimize --workspace "$(pwd)" --skill log-analysis-helper
optsmith version
optsmith help
```

## 1) Initialize project data folder

```bash
optsmith install --workspace "$(pwd)"
```

## 2) Auto-run one full workflow

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

## 2b) Auto-run with telemetry from runtime env

```bash
CODEX_TOTAL_TOKENS=1820 \
CODEX_TASK_DURATION_SEC=420 \
optsmith run --workspace "$(pwd)" \
  --task-id TASK-1002 \
  --task-type debug \
  --model gpt-5 \
  --used-skill true \
  --skill-name log-analysis-helper \
  --success true \
  --enforce-telemetry
```

If explicit telemetry is omitted, `optsmith run` will try to resolve values from local
Codex session logs (`$CODEX_HOME/sessions` and `$CODEX_HOME/archived_sessions`).

## 3) Log one task run (direct)

```bash
optsmith run --workspace "$(pwd)" \
  --task-id TASK-1001 \
  --task-type debug \
  --model gpt-5 \
  --used-skill true \
  --skill-name log-analysis-helper \
  --total-tokens 1820 \
  --duration-sec 420 \
  --success true \
  --skip-weekly
```

## 4) Measure skill effect

```bash
optsmith metrics --workspace "$(pwd)" --skill log-analysis-helper
```

## 5) Measure engineering pre/post effect

```bash
optsmith metrics --workspace "$(pwd)" --all --cutover 2026-03-01
```

## 6) Generate weekly review

Weekly review is included in `optsmith run` by default.
If you only want task logging + metrics without weekly review, add `--skip-weekly`.

## 7) Open the local dashboard

```bash
optsmith dashboard --workspace "$(pwd)" --host 127.0.0.1 --port 8765
```

## 8) Trigger optimization plan for one skill

```bash
optsmith optimize --workspace "$(pwd)" --skill log-analysis-helper
```
