# Munchkin Graph

`munchkin-graph` 提供跨 Compose Multiplatform 的共享图形能力。

主要包名：

- `munchkin.graph`
- `munchkin.graph.ninepatch`
- `munchkin.graph.lottie`
- `munchkin.graph.renderscript`

## 依赖方式

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:graph:<version>")
        }
    }
}
```

## 提供哪些能力

- 在共享 Compose UI 中解析和绘制 NinePatch
- `LottieResource` 与 `rememberLottiePainter`
- `Toolkit` 提供模糊等像素处理能力
- `Modifier.munchkinShadow` 提供软阴影绘制
- `ImagePadding`、`HasPaddingPainter`、`EmptyPainter` 等 Painter 工具

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

适合在共享 UI 中复用 Android `.9` 风格的拉伸区域。

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

`resource` 可以是生成的 raw 资源，也可以是引擎支持的其他资源来源。

## 模糊与 Toolkit

```kotlin
val output = Toolkit.blur(
    inputArray = rgbaBytes,
    vectorSize = 4,
    sizeX = width,
    sizeY = height,
    radius = 12,
)
```

Android 使用原生 RenderScript Toolkit 移植实现；iOS 使用平台原生实现。

## 阴影 Modifier

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
