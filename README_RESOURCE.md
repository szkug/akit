# Compose Multiplatform Resources Plugin (Akit)

This document explains the resource plugin design, implementation details, and usage notes
for iOS and Android.

## Overview

The plugin `cn.szkug.akit.resources` converts Android-style `src/res` into a unified `Res`
object and platform `ResourceId` values:
- Android: `ResourceId` maps to Android `R` ids.
- iOS: `ResourceId` is an `NSURL` that encodes a `compose-resources/<prefix>/...` path.

Only modules that own resources must apply the plugin. Pure consumer modules do not need it
unless they also generate their own `Res`. The iOS entry module (the one built by Xcode or
exported as a framework) must apply the plugin so it can sync all transitive resources.

## Generated output

Tasks and outputs (per module):
- `generateCmpResources`: generates `commonMain`, `androidMain`, `iosMain`, and `iosResources`
  into `build/generated/cmp-resources`.
- `prepareCmpComposeResources`: packages iOS resources into
  `build/generated/cmp-resources/compose-resources/<iosResourcesPrefix>`.
- `cmpComposeResourcesElements`: outgoing configuration that publishes the composed resources
  for dependency consumers.

## iOS runtime behavior

On iOS, `ResourceId` is a file URL containing a `prefix|relativePath` payload. At runtime
the loader:
- decodes the prefix and relative path,
- searches `NSBundle.mainBundle` under `compose-resources/<prefix>`,
- resolves locale (`.lproj`) and scale (`@2x/@3x`) variants,
- loads strings, images, and raw assets accordingly.

See `akit-libraries/resources-runtime/src/iosMain/.../ResourcesRuntime.ios.kt`.

## How resources are merged across modules

The plugin recursively collects `MainImplementation`/`MainApi` project dependencies and adds
each dependency's `cmpComposeResourcesElements` to `cmpComposeResourcesClasspath`. This means:
- each resource-owning module publishes its iOS resources,
- the iOS entry module aggregates all transitive resources into a single sync step,
- intermediate modules (without resources) do not need to apply the plugin.

## iOS sync strategy

### Xcode build

`syncCmpResourcesForXcode` runs during `embedAndSignAppleFrameworkForXcode` and copies
`compose-resources/<prefix>` into the app bundle directory determined by:
- `BUILT_PRODUCTS_DIR`
- `UNLOCALIZED_RESOURCES_FOLDER_PATH`

This works for the standard Xcode build flow.

### CocoaPods

When the `org.jetbrains.kotlin.native.cocoapods` plugin is present:
- resources are synced into `build/compose/cocoapods/compose-resources`,
- `syncCmpResourcesForXcode` is attached to `syncFramework`.

This aligns with Compose Multiplatform's CocoaPods resource layout.

### Manual output (CLI / framework export)

If you build outside Xcode or want a deterministic output:

```
./gradlew :your:module:syncCmpResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

Then copy `/path/to/Resources/compose-resources/<prefix>` into the final app bundle.

## Configuration

```kotlin
cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res"))
    packageName.set("com.example.app")
    androidNamespace.set("com.example.app")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosResourcesPrefix.set("cmp-res")
    iosExtraResDir.set(layout.projectDirectory.dir("src/iosMain/res"))
    whitelistEnabled.set(false)
    stringsWhitelistFile.set(layout.projectDirectory.file("res-whitelist/strings.txt"))
    drawablesWhitelistFile.set(layout.projectDirectory.file("res-whitelist/drawables.txt"))
}
```

## Notes and pitfalls

- Use a unique `iosResourcesPrefix` per module to avoid resource collisions.
- The iOS entry module must apply the plugin so `syncCmpResourcesForXcode` runs.
- If `src/res` does not exist, the plugin uses an empty directory (created by
  `prepareCmpEmptyResDir`).
- If resources are missing at runtime, check:
  - the bundle contains `compose-resources/<prefix>/...`,
  - the app target actually runs `embedAndSignAppleFrameworkForXcode` or `syncFramework`,
  - the correct prefix is set in both build and runtime output.

