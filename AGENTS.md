# AGENTS.md

## Mission

Use project-local skills and scripts to deliver high-quality outcomes with measurable engineering impact.
For code changes in this repository, prioritize module boundaries, Kotlin Multiplatform portability,
typed APIs, and predictable validation.

Project-local skills live under `./.agents/skills/`.

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

## Trigger Rules

Always load and use `akit-repo-standards` when any request matches one or more of these intents:

1. Modify or add code in `akit-libraries/*`, `apps/*`, `plugins/*`, or root Gradle settings.
2. Design or refactor architecture/dependency boundaries in this repository.
3. Add or adjust KMP `expect/actual`, Compose APIs, resources runtime/plugin behavior, image engines, or graph utilities.
4. Perform repository-level code review against local conventions and design constraints.

## Skill Execution Source of Truth

- For `akit-repo-standards`, follow execution rules in:
  - `./.agents/skills/akit-repo-standards/SKILL.md`
  - `./.agents/skills/akit-repo-standards/references/architecture-and-conventions.md` (for non-trivial changes)

## Definition of Done

A task is considered complete only when all applicable checks pass:

1. Requested code/doc change is complete.
2. Triggered skill workflows were followed based on their own `SKILL.md` rules.
3. If `akit-repo-standards` was triggered:
   - architecture/style constraints were applied,
   - at least one relevant module-level compile/test check ran (or a blocker is explicitly reported),
   - public-facing behavior changes are documented (or explicitly confirmed as not needed).

<!-- OPTSMITH-SKILL:START -->
## Agent Optsmith Integration
- skill: `agent-optsmith`
- skill_dir: `.agents/skills`
- data_dir: `.agents/optsmith-data`
- At task completion, run `optsmith run ...`.
<!-- OPTSMITH-SKILL:END -->
