# Munchkin Image

`munchkin-image` provides a shared async image API for Compose Multiplatform.

Main packages:

- `munchkin.image`
- `munchkin.image.coil`
- `munchkin.image.glide`

## Modules

- `image`
  Shared UI API such as `MunchkinAsyncImage`, `Modifier.munchkinAsyncBackground`, and `AsyncImageContext`
- `engine-coil`
  Coil 3 based engine for Android and iOS
- `engine-glide`
  Glide based engine for Android

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

## Choose An Engine

- Android + iOS: use `engine-coil`
- Android-only and want Glide integration: use `engine-glide`

## Main API

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = CoilRequestEngine.Normal,
)
```

On Android you can switch to Glide without changing the composable API:

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = GlideRequestEngine.Normal,
)
```

## Background Images

```kotlin
Text(
    text = "Hello",
    modifier = Modifier
        .munchkinAsyncBackground(
            model = "https://example.com/bg.9.png",
            context = rememberAsyncImageContext(
                supportNinepatch = true,
                ignoreImagePadding = true,
            ),
            engine = CoilRequestEngine.Normal,
        )
        .padding(8.dp)
)
```

## AsyncImageContext

`AsyncImageContext` controls request behavior:

- `logger` / `listener`
- `ignoreImagePadding`
- `animationIterations`
- `blurConfig`
- `sizeLimit`
- `supportNinepatch`
- `supportLottie`

Example:

```kotlin
val context = rememberAsyncImageContext(
    supportNinepatch = true,
    blurConfig = BlurConfig(radius = 12),
    sizeLimit = AsyncImageSizeLimit(maxWidth = 1080, maxHeight = 1080),
)
```

If you need `supportLottie` or a custom coroutine context, create `AsyncImageContext` directly.

## Common Model Types

Supported model types depend on the engine, but common cases include:

- HTTP URL `String`
- generated resource ids such as `Res.drawable.*` and `Res.raw.*`
- `PaintableResourceId`
- `Painter` and `ImageBitmap`
- `LottieResource`
- Android `Uri`, `File`, `Int` resource id, `Bitmap`, and `Drawable`

## Video Thumbnails

`engine-coil` supports loading a video source as a first-frame thumbnail on Android and iOS:

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/video.mp4",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = CoilRequestEngine.Normal,
)
```

## Binary Loading For Animation Modules

`CoilRequestEngine` and `GlideRequestEngine` can also provide binary source loading for animation modules such as `munchkin-svga`.

Use `MunchkinSvga(loadingEngine = yourImageEngine)` to reuse the same fetch and cache pipeline for remote `.svga` assets.
