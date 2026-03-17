# Munchkin Image

A shared async image API for Compose Multiplatform with Coil and Glide engines.

## Choose An Engine

- `engine-coil`: use this when you need Android + iOS support with one engine choice
- `engine-glide`: use this when you want Android-only integration with Glide

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:image:<version>")
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
```

## Basic Usage

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/banner.png",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = CoilRequestEngine.Normal,
)
```

On Android, you can switch to Glide without changing the UI API:

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/banner.png",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = GlideRequestEngine.Normal,
)
```

## Background Images

```kotlin
Box(
    modifier = Modifier
        .size(160.dp)
        .munchkinAsyncBackground(
            model = "https://example.com/cover.png",
            engine = CoilRequestEngine.Normal,
        )
)
```

## Request Options

```kotlin
val context = rememberAsyncImageContext(
    blurConfig = BlurConfig(radius = 12),
    sizeLimit = AsyncImageSizeLimit(maxWidth = 1080, maxHeight = 1080),
    supportNinepatch = true,
)

MunchkinAsyncImage(
    model = imageUrl,
    contentDescription = null,
    context = context,
    modifier = Modifier.size(160.dp),
    engine = CoilRequestEngine.Normal,
)
```

## Supported Models

The exact model types depend on the engine and platform, but common cases include:

- HTTP URL `String`
- generated resource ids such as `Res.drawable.*` or `Res.raw.*`
- `LottieResource`
- Android `Uri`, `File`, `Int` resource id, `Bitmap`, and `Drawable`

## What The Shared API Gives You

- `MunchkinAsyncImage` for image content
- `Modifier.munchkinAsyncBackground` for background rendering
- one request context model for blur, animation iterations, and size limiting
- engine-specific decoding kept out of your UI layer
