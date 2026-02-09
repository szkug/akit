# AKit Image

Akit Image provides Compose Multiplatform async image loading with a unified API. Engine
implementations live in separate modules so you can pick the platform backends you need.

## Features

- `AkitAsyncImage` composable and `Modifier.akitAsyncBackground`.
- Engine modules: Glide (Android) / Coil 3 (iOS).
- Supports `.9` NinePatch, GIF, and Lottie.
- Optional blur via `BlurConfig`.
- Pluggable `AsyncRequestEngine` for custom loaders.

## Engine modules

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:akit-image:$akitVersion")
        }
        androidMain.dependencies {
            implementation("cn.szkug.akit:akit-image-engine-glide:$akitVersion")
        }
        iosMain.dependencies {
            implementation("cn.szkug.akit:akit-image-engine-coil:$akitVersion")
        }
    }
}
```

## Main APIs

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
    engine: AsyncRequestEngine<*>
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
    engine: AsyncRequestEngine<*>
): Modifier
```

Common model types:

- URL / file / byte streams supported by the platform engine
- `PaintableResourceId` (from resources runtime)
- `Painter`, `ImageBitmap`
- `LottieResource` (from akit-graph)

## Usage

```kotlin
val engine = GlideRequestEngine.Normal // Android
// val engine = CoilRequestEngine.Normal // iOS

AkitAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
    engine = engine,
)
```

NinePatch background:

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
            engine = engine,
        )
        .padding(8.dp)
)
```

## AsyncImageContext

`AsyncImageContext` controls logging, listeners, animation, blur, and extensions:

```kotlin
val context = rememberAsyncImageContext(
    supportNinepatch = true,
    blurConfig = BlurConfig(radius = 12, repeat = 1),
)
```

Fields include:

- `logger` / `listener`
- `ignoreImagePadding`
- `animationIterations`
- `blurConfig`
- `supportNinepatch` / `supportLottie`

If you need a custom combination (for example `supportLottie`), create the context directly
instead of using `rememberAsyncImageContext`.

## Custom engine

You can provide a custom engine instance per call:

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

## Android large-bitmap guard

Akit ships a Glide transformation to prevent `draw too large bitmap` crashes. Configure it via
Glide options:

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
