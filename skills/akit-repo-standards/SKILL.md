---
name: akit-repo-standards
description: Apply AKit repository architecture and coding conventions for Kotlin Multiplatform, Compose, Gradle modules, and docs. Use when changing files under akit-libraries/apps/plugins, adding or refactoring modules/APIs, adjusting expect/actual implementations, or reviewing code for dependency boundaries, style consistency, and validation completeness.
---

# AKit Repo Standards

## Overview

Use this skill to keep repository changes aligned with AKit's module boundaries, KMP structure,
Compose/Kotlin API style, and documentation/validation expectations.

## Workflow

1. Classify the change scope before editing:
   - libraries: `akit-libraries/*`
   - apps/demos: `apps/*`
   - build logic: `plugins/*` or `akit-libraries/resources-gradle-plugin`
2. Load [references/architecture-and-conventions.md](references/architecture-and-conventions.md)
   for module map and enforcement checklist.
3. Apply architecture constraints first (dependency direction, source-set placement).
4. Implement with repository coding patterns (`expect/actual`, typed wrappers, Compose API style).
5. Run targeted validation command(s) for touched modules and summarize results.
6. Update docs in `docs/README_*` if public behavior/API changed.

## Enforcement Checklist

- [ ] Module placement matches responsibility (library/app/plugin).
- [ ] Dependency direction is preserved (`akit-image` stays engine-agnostic, engines stay in engine modules).
- [ ] `commonMain` vs platform source-set split is correct.
- [ ] Public APIs prefer typed models/value classes/sealed results over primitives.
- [ ] Compose APIs keep explicit/named parameters and existing defaults style.
- [ ] Validation command(s) executed or blockers clearly reported.
- [ ] Public-facing changes are documented or explicitly marked as no-doc-impact.

## Validation Guidance

Use the narrowest possible Gradle command for changed modules (compile/test/check) to validate
behavior without running full-repo tasks unless required.

## Resources

Read [references/architecture-and-conventions.md](references/architecture-and-conventions.md)
for the concrete module map, style patterns, decision matrix, and command examples.
