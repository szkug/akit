# Munchkin Graph

面向 Android 和 iOS 的 Compose Multiplatform 图形能力库。

## 适合什么场景

当你的 UI 需要下面这些能力时，可以接入 `munchkin-graph`：

- 在共享 Compose 代码里解析和绘制 NinePatch
- 以 `Painter` 的方式渲染 Lottie 文件
- 通过 `Modifier.munchkinShadow` 绘制软阴影
- 使用跨平台的模糊和图像处理能力

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

## 在共享 UI 中使用 NinePatch

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(imageBitmap, parsed.chunk ?: NinePatchChunk.createEmptyChunk())

Image(
    painter = painter,
    contentDescription = null,
)
```

## 把 Lottie 当作 Painter 使用

```kotlin
val painter = rememberLottiePainter(
    LottieResource(Res.raw.loading)
)

Image(
    painter = painter,
    contentDescription = null,
)
```

## 软阴影

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

## 平台说明

- Android 侧模糊和像素处理使用原生 toolkit 能力。
- iOS 侧使用平台原生实现，但共享 API 保持一致。
