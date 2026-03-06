# AKit Architecture And Conventions

## 1. Repository Topology

### Root Composition

- Gradle multi-project with Kotlin DSL
- Included builds:
  - `plugins` (local Gradle plugin build logic)
  - `akit-libraries/resources-gradle-plugin` (publishable plugin build)
- Feature preview enabled: `TYPESAFE_PROJECT_ACCESSORS`

### Primary Modules

1. Libraries (`akit-libraries/*`)
   - `resources-runtime`: shared typed resource APIs
   - `akit-graph`: NinePatch/Lottie/shadow/graphics toolkit
   - `akit-image`: engine-agnostic async image layer
   - `akit-image-engine-glide`: Android Glide engine
   - `akit-image-engine-coil`: Coil engine (Android/iOS)
2. Apps (`apps/*`)
   - `apps/cmp`: KMP demo host module
   - `apps/cmp-lib`, `apps/cmp-lib2`: shared demo/resource modules
   - `apps/android`: Android app integration/demo
3. Benchmark
   - `benchmark`: macrobenchmark target for `:apps:android`
4. Build Logic
   - `plugins/modules`: defines `cn.szkug.akit.alib`
   - `akit-libraries/resources-gradle-plugin`: defines `cn.szkug.akit.resources`

## 2. Dependency Boundary Rules

1. Keep `akit-image` engine-agnostic.
   - Allowed: shared Compose/image abstractions, graph/runtime helpers.
   - Not allowed: direct Glide/Coil implementation coupling.
2. Keep engine-specific behavior in `akit-image-engine-*`.
3. Keep shared resource access through generated `Res` + `resources-runtime`.
4. Keep plugin/runtime concerns separated.
   - Generation/sync logic in plugin modules.
   - Runtime APIs in library modules.
5. Demo behavior belongs in `apps/*`, not core libraries.

## 3. KMP Source-Set Pattern

1. Put portable contracts and most business logic in `commonMain`.
2. Use `expect/actual` for platform-specific capabilities.
3. Place platform files in explicit suffix files:
   - `*.android.kt`
   - `*.ios.kt`
4. For KMP Android manifests, keep:

```kotlin
sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
```

## 4. Gradle Conventions

1. Prefer version catalog aliases (`libs.*`) for external dependencies.
2. Prefer type-safe project accessors (`projects.*`) for internal dependencies.
3. Library modules typically apply:
   - `kotlin-multiplatform`
   - `org.jetbrains.compose`
   - `org.jetbrains.kotlin.plugin.compose`
   - `cn.szkug.akit.alib`
4. Prefer centralized Android SDK constants for libraries (`AndroidSdkVersions`).
5. Library KMP modules generally use `jvmToolchain(17)` unless the module already requires
   another version.

## 5. Kotlin And Compose Style

1. Package naming
   - `cn.szkug.akit.<domain>` for libraries
   - `cn.szkug.akit.apps.<domain>` for app/demo modules
2. API shape
   - Keep public composables explicit with named arguments.
   - Keep defaults centralized (`AsyncImageDefaults` style).
3. Type safety
   - Prefer typed IDs/wrappers (`ResourceId`, inline classes, sealed results).
   - Avoid exposing raw primitive values where a domain model exists.
4. Compose ergonomics
   - Prefer extension points for call-site clarity (`Modifier.akitAsyncBackground`, `toDp/toSp`).
5. Cross-platform behavior
   - Keep parity expectations explicit for Android and iOS.
   - Document intentional behavior gaps.

## 6. Design Heuristics By Area

### Resources

- Use generated `Res` access in shared code.
- Keep localization, plural, color, dimen logic in runtime/plugin layers.
- If changing resource generation shape, verify both Android and iOS outputs.

### Image

- Keep request pipeline abstractions in `akit-image`.
- Add new backend-specific decoders/transforms in corresponding engine module.
- Preserve placeholder/failure and animation lifecycle behavior.

### Graph

- Keep NinePatch/Lottie core APIs stable and type-safe.
- For native operations, preserve platform fallback behavior.

## 7. Change Decision Matrix

1. New cross-platform API:
   - Add contract in `commonMain`.
   - Add `actual` implementations per target as needed.
   - Expose typed API surface.
2. New image backend feature:
   - Add extension in engine module first.
   - Wire through shared abstractions only if broadly reusable.
3. Resource pipeline change:
   - Update plugin generation or runtime, not ad-hoc app-side logic.
4. Demo-only behavior:
   - Keep in `apps/*`.

## 8. Validation Commands (Examples)

Use the smallest command that validates the touched scope.

```bash
./gradlew :apps:android:compileDebugKotlin
./gradlew :apps:cmp:compileKotlinAndroid
./gradlew :akit-libraries:akit-image:compileKotlinAndroid
./gradlew :akit-libraries:resources-runtime:compileKotlinAndroid
```

For plugin/build logic changes, run the relevant plugin module compile/check task.

If a command cannot run in the current environment, report the blocker explicitly.

## 9. Documentation Update Rule

When public behavior changes, update corresponding docs:

- `docs/README_RESOURCE.md` / `docs/README_RESOURCE_CN.md`
- `docs/README_IMAGE.md` / `docs/README_IMAGE_CN.md`
- `docs/README_GRAPH.md` / `docs/README_GRAPH_CN.md`

If no doc change is needed, state why.
