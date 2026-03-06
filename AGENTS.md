# AGENTS.md

## Mission

Use project-local skills and scripts to deliver high-quality outcomes with measurable engineering impact.
For code changes in this repository, prioritize module boundaries, Kotlin Multiplatform portability,
typed APIs, and predictable validation.

## Repository Architecture Snapshot

AKit is a multi-project Gradle repository (Kotlin DSL) with included builds:

- build logic: `plugins` and `akit-libraries/resources-gradle-plugin`
- core libraries: `akit-libraries/*`
- apps and demos: `apps/*`
- perf verification: `benchmark`

Main module responsibilities:

1. `akit-libraries/resources-runtime`:
   - Typed resource IDs and runtime APIs (`stringResource`, `colorResource`, `painterResource`, `toDp`, `toSp`)
   - Android/iOS `expect/actual` runtime implementation
2. `akit-libraries/akit-graph`:
   - NinePatch parser/painter, Lottie painter, shadow helpers, native toolkit integration
3. `akit-libraries/akit-image`:
   - Engine-agnostic async image API for Compose Multiplatform
4. `akit-libraries/akit-image-engine-glide`:
   - Android-focused Glide engine implementation
5. `akit-libraries/akit-image-engine-coil`:
   - Coil-based engine with Android/iOS support
6. `apps/cmp`, `apps/cmp-lib`, `apps/cmp-lib2`, `apps/android`:
   - Integration demos and host applications
7. `plugins/modules`:
   - Local Gradle plugin `cn.szkug.akit.alib` for Android library defaults
8. `akit-libraries/resources-gradle-plugin`:
   - Gradle plugin `cn.szkug.akit.resources` for generating `Res` and syncing iOS resources

## Reusable Conventions

### Gradle + KMP

1. Prefer `libs.*` (version catalog) and `projects.*` (typesafe project accessors) for dependencies.
2. Keep KMP source-set split clear: `commonMain` for shared contracts, `androidMain`/`iosMain` for platform code.
3. Use `expect/actual` for platform behavior (e.g. runtime resources, engine selection, locale handling).
4. Keep Android manifest wired from `src/androidMain/AndroidManifest.xml` in KMP modules.
5. In library modules, prefer `AndroidSdkVersions` constants over ad-hoc SDK numbers.

### Kotlin + Compose

1. Keep package naming consistent (`cn.szkug.akit.<domain>` / `cn.szkug.akit.apps.<domain>`).
2. Public Compose APIs should use explicit named parameters and repository default constants when available.
3. Prefer strong types (`ResourceId`, inline wrappers, sealed results) over primitive leakage.
4. Prefer extension APIs for ergonomics (`Modifier.*`, `DimenResourceId.toDp`, etc.).
5. Platform-specific files should be explicit and localized (`*.android.kt`, `*.ios.kt`).

### Design Constraints

1. Preserve engine-agnostic boundary in `akit-image`; engine-specific logic must stay in `akit-image-engine-*`.
2. Keep cross-platform resource access through generated `Res` + `resources-runtime`.
3. Cross-platform features should define Android/iOS parity clearly (or explicitly document gaps).
4. Public module API changes should be reflected in `docs/README_RESOURCE*`, `docs/README_IMAGE*`, or `docs/README_GRAPH*`.

## Skill Source

Primary optimization skill is vendored via submodule:

- path: `.agent-loop/skills/agent-self-optimizing-loop/SKILL.md`

Repository standards skill:

- path: `skills/akit-repo-standards/SKILL.md`

## Trigger Rules

Always load and use `agent-self-optimizing-loop` when any request matches one or more of these intents:

1. Introduce or operate self-optimization workflow in this repository.
2. Log task token/duration/success/rework metrics.
3. Compare skill impact vs baseline (`token_reduction_pct`, `duration_reduction_pct`, success/rework deltas).
4. Run weekly incident/error review.
5. Analyze pre/post efficiency around a cutover date.
6. Discover optimization opportunities for existing skills.
7. Open or use the local dashboard for metric filtering and optimization triggering.

Always load and use `akit-repo-standards` when any request matches one or more of these intents:

1. Modify or add code in `akit-libraries/*`, `apps/*`, `plugins/*`, or root Gradle settings.
2. Design or refactor architecture/dependency boundaries in this repository.
3. Add or adjust KMP `expect/actual`, Compose APIs, resources runtime/plugin behavior, image engines, or graph utilities.
4. Perform repository-level code review against local conventions and design constraints.

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

When `akit-repo-standards` is triggered:

1. Load the skill:

```bash
cat skills/akit-repo-standards/SKILL.md
```

2. For non-trivial changes, load detailed reference:

```bash
cat skills/akit-repo-standards/references/architecture-and-conventions.md
```

3. Before editing, map requested changes to module boundaries (library/app/plugin/build-logic).
4. Before completion, run checklist validation from the skill (dependency direction, source-set placement, API typing, docs impact).

## Definition of Done

A task is considered complete only when all applicable checks pass:

1. Requested code/doc change is complete.
2. If optimization skill was triggered, at least one new row is added to `.agent-loop-data/metrics/task-runs.csv`.
3. For analysis/optimization requests, expected report artifacts are generated under `.agent-loop-data/reports/`.
4. If `akit-repo-standards` was triggered:
   - architecture/style constraints were applied,
   - at least one relevant module-level compile/test check ran (or a blocker is explicitly reported),
   - public-facing behavior changes are documented (or explicitly confirmed as not needed).
