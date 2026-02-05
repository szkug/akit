# AKit Image

Akit Image 提供 Compose Multiplatform 异步图片加载能力，Android 使用 Glide，iOS 使用
Coil 3。

## 功能概览

- `AkitAsyncImage` 与 `Modifier.akitAsyncBackground`。
- 平台引擎：Glide（Android）/ Coil 3（iOS）。
- 支持 `.9` NinePatch、GIF、Lottie。
- 可选高斯模糊（`BlurConfig`）。
- 可替换 `AsyncRequestEngine` 实现自定义加载器。

## 主要 API

```kotlin
@Composable
fun AkitAsyncImage(
    model: Any?,
    placeholder: Any? = null,
    failureRes: Any? = null,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    context: AsyncImageContext = rememberAsyncImageContext(),
    engine: AsyncRequestEngine<PlatformAsyncLoadData> = LocalPlatformAsyncRequestEngine.current
)
```

```kotlin
fun Modifier.akitAsyncBackground(
    model: Any?,
    placeholder: Any? = model,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    context: AsyncImageContext = rememberAsyncImageContext(supportNinepatch = true),
    engine: AsyncRequestEngine<PlatformAsyncLoadData> = LocalPlatformAsyncRequestEngine.current
): Modifier
```

常见 `model` 类型：

- URL / file / byte stream（由平台引擎支持）
- `PaintableResourceId`（resources-runtime）
- `Painter`、`ImageBitmap`
- `LottieResource`（akit-graph）

## 用法示例

```kotlin
AkitAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
)
```

NinePatch 背景：

```kotlin
Text(
    text = "Hello",
    modifier = Modifier
        .akitAsyncBackground(
            model = "https://example.com/bg.9.png",
            context = rememberAsyncImageContext(
                supportNinepatch = true,
                ignoreImagePadding = true,
            ),
        )
        .padding(8.dp)
)
```

## AsyncImageContext

`AsyncImageContext` 用于控制日志、监听器、动画、模糊、扩展支持等：

```kotlin
val context = rememberAsyncImageContext(
    supportNinepatch = true,
    blurConfig = BlurConfig(radius = 12, repeat = 1),
)
```

字段包含：

- `logger` / `listener`
- `ignoreImagePadding`
- `animationIterations`
- `blurConfig`
- `supportNinepatch` / `supportLottie`

如需设置 `supportLottie` 等高级参数，可直接创建 `AsyncImageContext` 实例。

## 自定义引擎

支持按调用传入或通过 `LocalPlatformAsyncRequestEngine` 全局替换：

```kotlin
// Android
val engine = GlideRequestEngine(
    requestBuilder = { ctx ->
        GlideApp.with(ctx.context).asDrawable()
    },
)

// iOS
val engine = CoilRequestEngine(
    factory = CoilImageLoaderSingletonFactory(),
)

AkitAsyncImage(
    model = url,
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = engine,
)
```

## Android 大图防护

Akit 内置 Glide 变换以避免 `draw too large bitmap` 崩溃，可通过 Glide 默认配置启用：

```kotlin
@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val metrics = context.resources.displayMetrics
        builder.setDefaultRequestOptions(
            RequestOptions().set(
                LargeBitmapLimitOption,
                LargeBitmapLimitConfig(metrics.widthPixels, metrics.heightPixels),
            ),
        )
    }
}
```
