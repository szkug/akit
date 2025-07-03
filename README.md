# AKit

Android kit libraries.

## Glide Toolkits

Load image for Compose base on Glide.

### Dependencies

```kotlin

val lastVersion = "1.0.5"

dependencies {
    // Compose supports
    implementation("cn.szkug.akit.glide:compose-image:${lastVersion}")
    
    // Blur decode libraryModule
    implementation("cn.szkug.akit.glide:extension-blur:${lastVersion}")

    // Ninepatch decode libraryModule
    implementation("cn.szkug.akit.glide:extension-ninepatch:${lastVersion}")
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

### Fix your build problem

If you use `extension-xxx` libraries and use an annotation processor such as kapt.
The relevant LibraryGlideModules will usually be registered automatically.

```kotlin
dependencies {
    kapt(libs.glide.compiler)
    implementation("cn.szkug.akit.glide:extension-blur:${lastVersion}")
    implementation("cn.szkug.akit.glide:extension-ninepatch:${lastVersion}")
}
```

In some projects, annotation processor may not find these LibraryGlideModules. You can try the following solutions to solve this problem.

Remove automatic Modules registration and call the `registerComponents` actively in GlideAppModuleImpl.

```kotlin
import com.bumptech.glide.annotation.Excludes

@GlideModule
@Excludes(value = [BlurBitmapLibraryGlideModule::class, NinePatchLibraryGlideModule::class])
class GlideAppModuleImpl : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {

        BlurBitmapLibraryGlideModule().registerComponents(context, glide, registry)
        NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
    }
}
```

If your project is compatible with a Java version, an error occurs when handing the `Excludes` annotation.
Try determine whether the `registerComponents` should be called according to `registerCount`.

```kotlin
@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        
        // If registerCount is 0, it means that the Module isn't registered automatically.
        if (BlurBitmapLibraryGlideModule.registerCount == 0) {
            BlurBitmapLibraryGlideModule().registerComponents(context, glide, registry)
        }

        if (NinePatchLibraryGlideModule.registerCount == 0) {
            NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
        }
    }
}
```

## Renderscript Toolkit

Publish submodule [renderscript-intrinsics-replacement-toolkit](https://github.com/korilin/renderscript-intrinsics-replacement-toolkit) to maven central.

```kotlin
dependencies {
    implementation("cn.szkug.akit:renderscript-toolkit:1.0.1")
}
```

CHANGELOG:
- 1.0.1: support 16KB page size