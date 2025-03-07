# AKit

Android kit libraries.

## Glide Toolkits

Load image for Compose base on Glide.

### Dependencies

```kotlin

val lastVersion = "1.0.2"

dependencies {
    // Compose supports
    implementation("io.github.korilin.akit.glide:compose-image:${lastVersion}")
    
    // Blur decode libraryModule
    implementation("io.github.korilin.akit.glide:extension-blur:${lastVersion}")

    // Ninepatch decode libraryModule
    implementation("io.github.korilin.akit.glide:extension-ninepatch:${lastVersion}")
}
```

### GlideAsyncImage Usage

For scenarios that display images from arbitrary resources, 
such as networks, files or resource ids.

```kotlin
GlideAsyncImage(
    modifier = Modifier.size(100.dp),
    model = model, // Any type supported by Glide
    contentDescription = null,
    contentScale = ContentScale.Crop,
    alignment = Alignment.Center
)
```

### GlideBackground Usage

For loading images as background.

```kotlin
Text(
    text = "Hello Kotlin.\nHello Compose!",
    color = Color.White,
    fontSize = 12.dp.sp,
    contentScale = ContentScale.Crop,
    alignment = Alignment.Center,
    modifier = Modifier
        .glideBackground(
            model = model, // Any type supported by Glide
            placeholder = placeholder,
            context = rememberAsyncImageContext(
                requestBuilder = {
                    Glide.with(it).asDrawable().skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .set(NinepatchEnableOption, true)
                }
            )
        )
)
```

### Custom RequestBuilder

You can build your custom Glide RequestBuilder in `AsyncImageContext` 
or use `rememberAsyncImageContext`.

```kotlin
open class AsyncImageContext(
    val context: Context,
    val enableLog: Boolean = false,
    val requestBuilder: (Context) -> RequestBuilder<Drawable> = NormalGlideRequestBuilder,

    // internal support fields
    val ignoreImagePadding: Boolean = false,

    // transformations
    val bitmapTransformations: List<BitmapTransformation>? = null,
    val drawableTransformations: List<DrawableTransformation>? = null,
)

rememberAsyncImageContext(
    // Build your custom RequestBuilder
    requestBuilder = { context: Context ->
        AsyncImageContext.NormalGlideRequestBuilder(context).skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

            // Need to import extension-blur to use BlurBitmapConfigOption
            .set(BlurBitmapConfigOption, BlurConfig(15))
            
            // If use glideBackground to load as the background,
            // Import extension-ninepatch library to use NinepatchEnableOption,
            // to enable ninepatch loading support
            .set(NinepatchEnableOption, true)
    }
)
```

### Limit bitmap size

AKit limits the size of bitmps internally, to prevent `draw too large bitmap` crash.

If you want to further restrict the bitmap width and height, you can set LargeBitmapLimitConfig 
in AppGlideModule using setDefaultRequestOptions.

```kotlin
@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {

        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels

        builder.setDefaultRequestOptions(
            RequestOptions().format(DecodeFormat.PREFER_ARGB_8888).set(
                LargeBitmapLimitOption, LargeBitmapLimitConfig(widthPixels, heightPixels)
            )
        )
    }
}
```
