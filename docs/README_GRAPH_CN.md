# AKit Graph

Akit Graph 提供跨 Compose Multiplatform 的图形工具集，包括 NinePatch 解析、Lottie
Painter 以及 RenderScript Toolkit。

## 功能概览

- NinePatch 解析与绘制：
  - `NinePatchChunk` / `NinePatchParser` / `NinePatchPainter`
  - 支持原始 1px 边框与 chunk 两种形式
- Lottie Painter：
  - `LottieResource` 与 `rememberLottiePainter`
- RenderScript Toolkit（Android 原生 + iOS Accelerate）：
  - blur、blend、color matrix、convolve、histogram、LUT、resize、YUV->RGB
- 视觉工具：
  - `Modifier.akitShadow` 软阴影绘制
- Painter 工具：
  - `HasPaddingPainter`、`ImagePadding`、`EmptyPainter`

## NinePatch 示例

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(imageBitmap, parsed.chunk ?: NinePatchChunk.createEmptyChunk())

Image(painter = painter, contentDescription = null)
```

`NinePatchPainter` 提供 `ImagePadding` 方便布局处理。

## Lottie Painter

```kotlin
val painter = rememberLottiePainter(
    LottieResource(Res.raw.loading, iterations = LottieIterations.Forever)
)
Image(painter = painter, contentDescription = null)
```

Android 使用 Lottie Android SDK；iOS 会解析 JSON 并进行原生渲染。

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

Android 使用 RenderScript Toolkit 的 C++ 实现；iOS 使用 Accelerate，并在部分场景下
回退到 Kotlin 实现。

## 阴影 Modifier

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
