# Munchkin Sample Architecture And Conventions

## 1. Repository Topology

### Root Composition

- Gradle multi-project with Kotlin DSL
- Included build:
  - `plugins` (local Gradle plugin build logic, also hosts the resources Gradle plugin)
- In-repo library modules under `libs/*`
- Feature preview enabled: `TYPESAFE_PROJECT_ACCESSORS`

### Primary Modules

1. Extracted libraries (`libs/*`)
   - `libs/graph`: NinePatch/Lottie/shadow/graphics toolkit
   - `libs/image:image`: engine-agnostic async image layer
   - `libs/image:engine-coil`: Coil engine (Android/iOS)
   - `libs/image:engine-glide`: Glide engine (Android)
   - `libs/resource:runtime`: shared typed resource APIs
   - `libs/resource:gradle-plugin`: resource generation and iOS sync plugin
2. Apps (`apps/*`)
   - `apps/cmp`: KMP demo host module
   - `apps/cmp-lib`, `apps/cmp-lib2`: shared demo/resource modules
   - `apps/android`: Android app integration/demo
3. Benchmark
   - `benchmark`: macrobenchmark target for `:apps:android`
4. Build Logic
   - `plugins/modules`: defines `cn.szkug.munchkin.alib`

## 2. Dependency Boundary Rules

1. Keep `libs/image:image` engine-agnostic.
   - Allowed: shared Compose/image abstractions, graph/runtime helpers.
   - Not allowed: direct Glide/Coil implementation coupling.
2. Keep engine-specific behavior in `libs/image:engine-*`.
3. Keep shared resource access through generated `Res` plus `libs/resource:runtime`.
4. Keep plugin/runtime concerns separated.
   - Generation and sync logic in `libs/resource:gradle-plugin`.
   - Runtime APIs in `libs/resource:runtime`.
5. Demo behavior belongs in `apps/*`, not extracted library modules.

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
2. Prefer type-safe project accessors (`projects.*`) for in-repo modules and sample modules.
3. Library and shared KMP modules generally apply:
   - `kotlin-multiplatform`
   - `org.jetbrains.compose`
   - `org.jetbrains.kotlin.plugin.compose`
4. Root sample modules that need Android library defaults should use `cn.szkug.munchkin.alib`.
5. Use `jvmToolchain(17)` unless a touched module already requires another version.

## 5. Kotlin And Compose Style

1. Package naming
   - `munchkin.<domain>` for libraries
   - `munchkin.apps.<domain>` for demo modules
   - `munchkin.sample.<domain>` for sample app and benchmark code
2. API shape
   - Keep public composables explicit with named arguments.
   - Keep defaults centralized (`AsyncImageDefaults` style).
3. Type safety
   - Prefer typed IDs and wrappers (`ResourceId`, inline classes, sealed results).
   - Avoid exposing raw primitive values where a domain model exists.
4. Compose ergonomics
   - Prefer extension points for call-site clarity (`Modifier.munchkinAsyncBackground`, `toDp`, `toSp`).
5. Cross-platform behavior
   - Keep parity expectations explicit for Android and iOS.
   - Document intentional behavior gaps.

## 6. Design Heuristics By Area

### Resources

- Use generated `Res` access in shared code.
- Keep localization, plural, color, dimen logic in runtime/plugin layers.
- If changing resource generation shape, verify both Android and iOS outputs.

### Image

- Keep request pipeline abstractions in `libs/image:image`.
- Add new backend-specific decoders or transforms in the corresponding engine module.
- Preserve placeholder/failure and animation lifecycle behavior.

### Graph

- Keep NinePatch/Lottie core APIs stable and type-safe.
- For native operations, preserve platform fallback behavior.

### Sample Apps

- Keep app-only wiring, demo data, and showcase flows inside `apps/*`.
- Prefer consuming in-repo libraries through project dependencies during workspace development.

## 7. Change Decision Matrix

1. New cross-platform API:
   - Add the contract in the appropriate extracted library `commonMain` source set.
   - Add `actual` implementations per target as needed.
   - Expose a typed API surface.
2. New image backend feature:
   - Add the extension in `libs/image:engine-*` first.
   - Wire through shared abstractions only if broadly reusable.
3. Resource pipeline change:
   - Update `libs/resource:gradle-plugin` or `libs/resource:runtime`, not ad-hoc app-side logic.
4. Demo-only behavior:
   - Keep it in `apps/*`.

## 8. Validation Commands (Examples)

Use the smallest command that validates the touched scope.

```bash
./gradlew :apps:android:compileDebugKotlin
./gradlew :apps:cmp:compileDebugKotlinAndroid
./gradlew :benchmark:compileBenchmarkKotlin
./gradlew :libs:graph:allTests :libs:graph:assembleDebug :libs:graph:compileKotlinIosSimulatorArm64
./gradlew :libs:resource:runtime:compileDebugKotlinAndroid :libs:resource:runtime:compileKotlinIosSimulatorArm64
./gradlew :libs:image:image:allTests :libs:image:engine-coil:allTests :libs:image:engine-glide:compileDebugKotlinAndroid :libs:image:image:compileKotlinIosSimulatorArm64 :libs:image:engine-coil:compileKotlinIosSimulatorArm64
./gradlew -p plugins :resource-gradle-plugin:test
```

If a command cannot run in the current environment, report the blocker explicitly.

## 9. Documentation Update Rule

When public behavior changes, update the corresponding docs where that code now lives:

- `libs/graph/README.md` / `libs/graph/README_CN.md`
- `libs/image/README.md` / `libs/image/README_CN.md`
- `libs/resource/README.md` / `libs/resource/README_CN.md`
- `README.md` / `README_CN.md` for root sample workspace behavior

If no doc change is needed, state why.
