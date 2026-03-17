# Munchkin Graph

Compose Multiplatform graphics helpers shared across Android and iOS.

## Features

- NinePatch parsing and rendering
- Lottie painter support
- RenderScript Toolkit bridge on Android and Accelerate-backed processing on iOS
- `Modifier.munchkinShadow` soft shadow helper
- Painter utilities for Compose image rendering

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:graph:<version>")
        }
    }
}
```

## Publish

```bash
./gradlew publishToMavenLocal
./gradlew publishToMavenCentral
./gradlew publishAndReleaseToMavenCentral
```

Remote publishing requires Maven Central credentials and in-memory GPG signing via Gradle properties or environment variables.

## Key APIs

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(imageBitmap, parsed.chunk ?: NinePatchChunk.createEmptyChunk())

Image(painter = painter, contentDescription = null)
```

```kotlin
val painter = rememberLottiePainter(
    LottieResource(Res.raw.loading, iterations = LottieIterations.Forever)
)
Image(painter = painter, contentDescription = null)
```

```kotlin
Box(
    modifier = Modifier
        .size(120.dp)
        .munchkinShadow(
            color = Color.Black.copy(alpha = 0.3f),
            effect = 16.dp,
            offset = DpOffset(0.dp, 6.dp),
        )
)
```
