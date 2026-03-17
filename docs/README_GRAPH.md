# Munchkin Graph

`munchkin-graph` provides shared graphics helpers for Compose Multiplatform.

Main packages:

- `munchkin.graph`
- `munchkin.graph.ninepatch`
- `munchkin.graph.lottie`
- `munchkin.graph.renderscript`

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

## What It Provides

- NinePatch parsing and drawing in shared Compose UI
- `LottieResource` and `rememberLottiePainter`
- `Toolkit` image-processing helpers for blur and other pixel operations
- `Modifier.munchkinShadow` for soft shadow rendering
- painter helpers such as `ImagePadding`, `HasPaddingPainter`, and `EmptyPainter`

## NinePatch

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(
    imageBitmap,
    parsed.chunk ?: NinePatchChunk.createEmptyChunk(),
)

Image(
    painter = painter,
    contentDescription = null,
)
```

Use this when you need Android `.9`-style stretch regions in shared UI.

## Lottie Painter

```kotlin
val painter = rememberLottiePainter(
    LottieResource(
        resource = Res.raw.loading,
        iterations = LottieIterations.Forever,
    )
)

Image(
    painter = painter,
    contentDescription = null,
)
```

`resource` can be a generated raw resource id or another engine-supported resource source.

## Blur And Toolkit

```kotlin
val output = Toolkit.blur(
    inputArray = rgbaBytes,
    vectorSize = 4,
    sizeX = width,
    sizeY = height,
    radius = 12,
)
```

Android uses the native RenderScript Toolkit port. iOS uses platform-native implementations while
keeping the API shape the same.

## Shadow Modifier

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
