# Munchkin Image

面向 Compose Multiplatform 的统一异步图片 API，底层可选 Coil 或 Glide 引擎。

## 如何选择引擎

- `engine-coil`：需要一套引擎同时覆盖 Android 和 iOS 时使用
- `engine-glide`：只做 Android 接入，并且希望复用 Glide 生态时使用

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

## 基础用法

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/banner.png",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = CoilRequestEngine.Normal,
)
```

在 Android 上，如果你想切到 Glide，UI API 不需要改：

```kotlin
MunchkinAsyncImage(
    model = "https://example.com/banner.png",
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = GlideRequestEngine.Normal,
)
```

## 背景图加载

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

## 请求上下文能力

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

## 支持的模型类型

具体支持范围取决于引擎和平台，常见场景包括：

- HTTP URL `String`
- `Res.drawable.*`、`Res.raw.*` 这类生成资源
- `LottieResource`
- Android 下的 `Uri`、`File`、`Int` 资源 ID、`Bitmap`、`Drawable`

## 共享 API 能带来什么

- 用 `MunchkinAsyncImage` 渲染图片内容
- 用 `Modifier.munchkinAsyncBackground` 渲染背景图
- 用同一套请求上下文配置模糊、动画次数、尺寸限制
- 把引擎特有解码逻辑隔离在 UI 之外
