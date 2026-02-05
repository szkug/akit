# AKit Graph

Akit Graph provides graphics utilities shared across Compose Multiplatform targets, including
NinePatch parsing, Lottie painter support, and the RenderScript Toolkit port.

## Features

- NinePatch parsing and rendering:
  - `NinePatchChunk` / `NinePatchParser` / `NinePatchPainter`
  - Raw NinePatch border parsing (1px border) and chunk parsing
- Lottie painter:
  - `LottieResource` and `rememberLottiePainter`
- RenderScript Toolkit (Android native + iOS Accelerate):
  - blur, blend, color matrix, convolve, histogram, LUT, resize, YUV->RGB
- Visual helper:
  - `Modifier.akitShadow` for soft shadow drawing
- Painter utilities:
  - `HasPaddingPainter`, `ImagePadding`, `EmptyPainter`

## NinePatch usage

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(imageBitmap, parsed.chunk ?: NinePatchChunk.createEmptyChunk())

Image(painter = painter, contentDescription = null)
```

`NinePatchPainter` exposes `ImagePadding` for layout adjustments when needed.

## Lottie painter

```kotlin
val painter = rememberLottiePainter(
    LottieResource(Res.raw.loading, iterations = LottieIterations.Forever)
)
Image(painter = painter, contentDescription = null)
```

Android uses the Lottie Android SDK; iOS resolves the JSON file path and renders it natively.

## RenderScript Toolkit

```kotlin
val output = Toolkit.blur(
    inputArray = rgbaBytes,
    vectorSize = 4,
    sizeX = width,
    sizeY = height,
    radius = 12,
)
```

Android uses a native C++ port of RenderScript Toolkit. iOS uses Accelerate where available and
falls back to Kotlin implementations for unsupported operations.

## Shadow modifier

```kotlin
Box(
    modifier = Modifier
        .size(120.dp)
        .akitShadow(
            color = Color.Black.copy(alpha = 0.3f),
            effect = 16.dp,
            offset = DpOffset(0.dp, 6.dp),
        )
)
```
