# AGENTS.md

## Mission

Use project-local skills and scripts to deliver high-quality outcomes with measurable engineering impact.
For code changes in this repository, prioritize module boundaries, Kotlin Multiplatform portability,
typed APIs, predictable validation, and clean sample-to-library separation.

## Repository Architecture Snapshot

Munchkin Cats is a Gradle multi-project workspace (Kotlin DSL) with in-repo library modules under `libs/` plus an included build for local build logic.

- build logic: `plugins`
- library modules: `libs/graph`, `libs/image/*`, `libs/resource/*`
- apps and demos: `apps/*`
- perf verification: `benchmark`

Main responsibilities:

1. `libs/graph`
   - Graphics toolkit: NinePatch parser/painter, Lottie painter, shadow helpers, native toolkit integration
2. `libs/image`
   - `:image`: engine-agnostic async image API for Compose Multiplatform
   - `:engine-coil`: Coil-based engine with Android/iOS support
   - `:engine-glide`: Android-focused Glide engine implementation
3. `libs/resource`
   - `:runtime`: typed resource IDs and runtime APIs (`stringResource`, `colorResource`, `painterResource`, `toDp`, `toSp`)
   - `:gradle-plugin`: Gradle plugin `cn.szkug.munchkin.resources` for generating `Res` and syncing iOS resources
4. `apps/cmp`, `apps/cmp-lib`, `apps/cmp-lib2`, `apps/android`
   - Integration demos and host applications for the in-repo libraries
5. `plugins/modules`
   - Local Gradle plugin `cn.szkug.munchkin.alib` for Android library defaults used by the sample workspace
6. `benchmark`
   - Macrobenchmark verification for `apps/android`

## Reusable Conventions

### Gradle + KMP

1. Prefer `libs.*` for external dependencies; prefer `projects.*` for in-repo modules.
2. Keep KMP source-set split clear: `commonMain` for shared contracts, `androidMain`/`iosMain` for platform code.
3. Use `expect/actual` for platform behavior such as runtime resources, engine selection, and locale handling.
4. Keep Android manifests wired from `src/androidMain/AndroidManifest.xml` in KMP modules.
5. In shared modules, prefer centralized SDK/toolchain configuration over ad-hoc per-file settings.

### Kotlin + Compose

1. Keep package naming consistent (`munchkin.<domain>`, `munchkin.apps.<domain>`, `munchkin.sample.<domain>`).
2. Public Compose APIs should use explicit named parameters and repository default constants when available.
3. Prefer strong types (`ResourceId`, inline wrappers, sealed results) over primitive leakage.
4. Prefer extension APIs for ergonomics (`Modifier.*`, `toDp`, `toSp`, etc.).
5. Platform-specific files should be explicit and localized (`*.android.kt`, `*.ios.kt`).

### Design Constraints

1. Preserve the engine-agnostic boundary in `libs/image:image`; engine-specific logic must stay in `libs/image:engine-*`.
2. Keep cross-platform resource access through generated `Res` plus `libs/resource:runtime`.
3. Cross-platform features should define Android/iOS parity clearly, or explicitly document gaps.
4. Public library API or behavior changes should update the corresponding module `README.md` / `README_CN.md`; root docs should describe workspace-level behavior.

## Trigger Rules

Always load and use `akit-repo-standards` when any request matches one or more of these intents:

1. Modify or add code in `libs/*`, `apps/*`, `plugins/*`, `benchmark`, or root Gradle settings.
2. Design or refactor architecture/dependency boundaries in this workspace.
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
   - at least one relevant module-level compile/test check ran for each touched module (or a blocker is explicitly reported),
   - public-facing behavior changes are documented in the corresponding root or module README files (or explicitly confirmed as not needed).

<!-- OPTSMITH-SKILL:START -->
## Agent Optsmith Integration
- skill: `agent-optsmith`
- skill_dir: `.agents/skills`
- data_dir: `.agents/optsmith-data`
- At task completion, run `optsmith run ...`.
<!-- OPTSMITH-SKILL:END -->

## Code Design
- All code should include comments on class, function, and field definitions, and those comments should be written in English.
- Comments should cover: the responsibility/purpose, the implementation approach (not required for very simple logic), and the related business context (except for fully generic components).
- For relatively complex logic or logic with many branches, document each branch in detail, explain the purpose of the concrete implementation, and list all logical branches at the top-level function that owns the branching logic.
- For code that specifically fixes scenario-driven issues (such as bug, crash, or ANR fixes), clearly explain what problem is being fixed, why it happens, and how the code addresses it.
- Demo no need comments.

For an example of the expected engineering code style, refer to [AGENTS_CODE_DEMO.kt](AGENTS_CODE_DEMO.kt)
