---
name: akit-repo-standards
description: Apply the Munchkin sample workspace architecture and coding conventions for Kotlin Multiplatform, Compose, Gradle modules, extracted submodules, and docs. Use when changing files under libs/apps/plugins/benchmark, adding or refactoring modules/APIs, adjusting expect/actual implementations, or reviewing code for dependency boundaries, style consistency, and validation completeness.
---

# AKit Repo Standards

## Overview

Use this skill to keep workspace and submodule changes aligned with the Munchkin sample topology,
KMP structure, Compose/Kotlin API style, and documentation/validation expectations.

## Workflow

1. Classify the change scope before editing:
   - extracted libraries: `libs/graph`, `libs/image`, `libs/resource`
   - apps/demos: `apps/*`
   - benchmark: `benchmark`
   - build logic: `plugins/*`
2. Load [references/architecture-and-conventions.md](references/architecture-and-conventions.md)
   for the module map and enforcement checklist.
3. Apply architecture constraints first (dependency direction, source-set placement, sample-vs-library separation).
4. Implement with repository coding patterns (`expect/actual`, typed wrappers, Compose API style).
5. Run targeted validation command(s) for touched modules and summarize results.
6. Update the relevant root or submodule `README.md` / `README_CN.md` if public behavior or integration guidance changed.

## Enforcement Checklist

- [ ] Module placement matches responsibility (submodule library vs sample app vs build logic).
- [ ] Dependency direction is preserved (`libs/image:image` stays engine-agnostic, engines stay in engine modules).
- [ ] `commonMain` vs platform source-set split is correct.
- [ ] Public APIs prefer typed models/value classes/sealed results over primitives.
- [ ] Compose APIs keep explicit/named parameters and existing defaults style.
- [ ] Validation command(s) executed or blockers clearly reported.
- [ ] Public-facing changes are documented or explicitly marked as no-doc-impact.

## Validation Guidance

Use the narrowest possible Gradle command for changed modules (compile/test/check) to validate
behavior without running full-workspace tasks unless required.

## Resources

Read [references/architecture-and-conventions.md](references/architecture-and-conventions.md)
for the concrete module map, style patterns, decision matrix, and command examples.
