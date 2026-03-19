# Munchkin Cats

[中文文档](./README_CN.md)

Component integration guide for the Munchkin Compose Multiplatform libraries.

## Modules

- Resource
  Generate typed `Res.*` accessors from Android-style resources and read them on Android/iOS.
  Guide: [docs/README_RESOURCE.md](./docs/README_RESOURCE.md)
- Image
  Shared async image API with Coil and Glide engines.
  Guide: [docs/README_IMAGE.md](./docs/README_IMAGE.md)
- Graph
  NinePatch, Lottie painter, blur helpers, and shadow rendering.
  Guide: [docs/README_GRAPH.md](./docs/README_GRAPH.md)
- SVGA
  Cross-platform SVGA playback with self-hosted decode, dynamic text/image replacement, audio, and click areas.
  Guide: [docs/README_SVGA.md](./docs/README_SVGA.md)

## Quick Setup

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:runtime:<version>")
            implementation("cn.szkug.munchkin:image:<version>")
            implementation("cn.szkug.munchkin:graph:<version>")
            implementation("cn.szkug.munchkin:svga:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
            // or implementation("cn.szkug.munchkin:engine-glide:<version>")
        }
        iosMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
    }
}

plugins {
    id("cn.szkug.munchkin.resources") version "<version>"
}
```

## Which Guide Should You Read?

- Need generated `Res.*`, `stringResource`, `painterResource`, `toDp`, or `toSp`?
  Start with [docs/README_RESOURCE.md](./docs/README_RESOURCE.md)
- Need `MunchkinAsyncImage`, background image loading, Glide, or Coil?
  Start with [docs/README_IMAGE.md](./docs/README_IMAGE.md)
- Need NinePatch, `rememberLottiePainter`, `Toolkit`, or `Modifier.munchkinShadow`?
  Start with [docs/README_GRAPH.md](./docs/README_GRAPH.md)
- Need SVGA playback, dynamic text/image replacement, audio timeline, or click hit testing?
  Start with [docs/README_SVGA.md](./docs/README_SVGA.md)
