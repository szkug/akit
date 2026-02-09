# AKit

[中文文档](./README_CN.md)

Compose Multiplatform utilities for shared resources, image loading, and graphics helpers.

Current version: last_version

## Modules

- Resources (plugin + runtime): generate `Res` and typed `ResourceId` for cross-platform resources.
  Details: [docs/README_RESOURCE.md](./docs/README_RESOURCE.md)
- Image: async image loading with NinePatch, Lottie, GIF, and blur support (engines in `akit-image-engine-glide` / `akit-image-engine-coil`).
  Details: [docs/README_IMAGE.md](./docs/README_IMAGE.md)
- Graph: NinePatch parsing/painter, Lottie painter, RenderScript Toolkit, shadow modifier.
  Details: [docs/README_GRAPH.md](./docs/README_GRAPH.md)

## Install

```kotlin
val last_version = "last_version"
```

Runtime libraries:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:resources-runtime:$last_version")
            implementation("cn.szkug.akit:akit-image:$last_version")
            implementation("cn.szkug.akit:akit-graph:$last_version")
        }
    }
}
```

Resources plugin (apply only to resource-owning modules; iOS entry module must apply it to sync
transitive resources):

```kotlin
plugins {
    id("cn.szkug.akit.resources") version last_version
}
```

## Quick usage

```kotlin
Text(text = stringResource(Res.strings.app_name))
Text(text = pluralStringResource(Res.strings.common_hours, 2, 2))
Box(Modifier.background(colorResource(Res.colors.primary)))
Image(painter = painterResource(Res.drawable.logo), contentDescription = null)
```

```kotlin
val engine = GlideRequestEngine.Normal // Android
// val engine = CoilRequestEngine.Normal // iOS

AkitAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = engine,
)
```

```kotlin
val lottiePainter = rememberLottiePainter(LottieResource(Res.raw.loading))
Image(painter = lottiePainter, contentDescription = null)
```
