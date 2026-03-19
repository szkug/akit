# Munchkin Image

`munchkin-image` 提供一套面向 Compose Multiplatform 的统一异步图片 API。

主要包名：

- `munchkin.image`
- `munchkin.image.coil`
- `munchkin.image.glide`

## 模块说明

- `image`
  提供 `MunchkinAsyncImage`、`Modifier.munchkinAsyncBackground`、`AsyncImageContext` 等共享 UI API
- `engine-coil`
  基于 Coil 3 的 Android / iOS 双端引擎
- `engine-glide`
  基于 Glide 的 Android 引擎

## 依赖方式

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:image:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
            // 或 implementation("cn.szkug.munchkin:engine-glide:<version>")
        }
        iosMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
    }
}
```

## 如何选择引擎

- Android + iOS：使用 `engine-coil`
- 只做 Android，且想接 Glide：使用 `engine-glide`

## 主 API

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = CoilRequestEngine.Normal,
)
```

在 Android 上，你也可以切到 Glide，而不用改 Composable API：

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = GlideRequestEngine.Normal,
)
```

## 背景图

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

`AsyncImageContext` 用于控制请求行为：

- `logger` / `listener`
- `ignoreImagePadding`
- `animationIterations`
- `blurConfig`
- `sizeLimit`
- `supportNinepatch`
- `supportLottie`

示例：

```kotlin
val context = rememberAsyncImageContext(
    supportNinepatch = true,
    blurConfig = BlurConfig(radius = 12),
    sizeLimit = AsyncImageSizeLimit(maxWidth = 1080, maxHeight = 1080),
)
```

如果你需要 `supportLottie` 或自定义协程上下文，直接创建 `AsyncImageContext` 即可。

## 常见 model 类型

具体支持范围取决于引擎，常见场景包括：

- HTTP URL `String`
- `Res.drawable.*`、`Res.raw.*` 这类生成资源
- `PaintableResourceId`
- `Painter`、`ImageBitmap`
- `LottieResource`
- Android 下的 `Uri`、`File`、`Int` 资源 ID、`Bitmap`、`Drawable`

## 视频首帧缩略图

`engine-coil` 支持在 Android 和 iOS 上把视频资源作为首帧缩略图加载：

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/video.mp4",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = CoilRequestEngine.Normal,
)
```

## 动画模块的二进制加载

`CoilRequestEngine` 和 `GlideRequestEngine` 也可以给 `munchkin-svga` 这类动画模块提供二进制资源加载能力。

通过 `MunchkinSvga(loadingEngine = yourImageEngine)` 可以复用同一套网络下载和缓存链路来加载远端 `.svga` 资源。
