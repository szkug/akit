# Munchkin Graph

跨 Android / iOS 共享的 Compose Multiplatform 图形辅助库。

## 功能

- NinePatch 解析与绘制
- Lottie Painter
- Android RenderScript Toolkit / iOS Accelerate 图像处理能力
- `Modifier.munchkinShadow` 软阴影能力
- Compose Painter 辅助工具

## 依赖

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:graph:<version>")
        }
    }
}
```

## 发布

```bash
./gradlew publishToMavenLocal
./gradlew publishToMavenCentral
./gradlew publishAndReleaseToMavenCentral
```

远端发布需要通过 Gradle 属性或环境变量提供 Maven Central 凭据和内存 GPG 签名信息。

## 关键 API

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
