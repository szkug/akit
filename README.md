# AKit

[中文文档](./README_CN.md)

Compose Multiplatform helpers for image loading, NinePatch rendering, and resource access.

Current version: 2.0.0-CMP-11

Artifacts (group `cn.szkug.akit`):
- `akit-image`: Compose Multiplatform async image loader (Glide on Android, Coil 3 on iOS).
- `akit-graph`: shared graphics helpers (NinePatch parsing/painter).
- `resources-runtime`: Compose Multiplatform ResourceId runtime.

Gradle plugin: `cn.szkug.akit.resources` (group `cn.szkug.akit.resources`).

## Akit Image (Compose Multiplatform)

Akit Image provides `AkitAsyncImage` and `Modifier.akitAsyncBackground`. It bridges platform image
loaders into a Compose `Painter` via `AsyncRequestEngine`.

Additional supported image types:
- .9 images
- GIF animations

### Usage

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:akit-image:$lastVersion")
        }
    }
}
```

`AsyncImageContext` works as the image loading context and provides logging, listeners, and
extension switches.

```kotlin
@Composable
fun rememberAsyncImageContext(
    vararg keys: Any?,
    ignoreImagePadding: Boolean = false,
    logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    listener: AsyncImageLoadListener? = null,
    animationContext: CoroutineContext = rememberCoroutineScope().coroutineContext,
    // extension support
    supportNinepatch: Boolean = false,
): AsyncImageContext {

    val platformContext = LocalPlatformImageContext.current
    return remember(ignoreImagePadding, supportNinepatch, animationContext, *keys) {
        AsyncImageContext(
            context = platformContext,
            ignoreImagePadding = ignoreImagePadding,
            logger = logger,
            listener = listener,
            coroutineContext = animationContext,
            supportNinepatch = supportNinepatch
        )
    }
}
```

```kotlin
AkitAsyncImage(
    model = url,
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    contentScale = ContentScale.Crop,
    context = rememberAsyncImageContext(),
    engine = YourCustomEngine // You can customize the engine. Android uses Glide, iOS uses Coil.
)
```

`akitAsyncBackground` enables .9 support by default and adapts to the .9 content padding. If you
want the layout size to ignore .9 padding, set `ignoreImagePadding = true`.

```kotlin
Text(
    text = "Hello Compose",
    modifier = Modifier
        .akitAsyncBackground(
            model = url,
            placeholder = Res.drawable.nine_patch_2,
            contentScale = ContentScale.FillBounds,
            context = rememberAsyncImageContext(
                supportNinepatch = true,
                ignoreImagePadding = true
            ),
        )
        .padding(8.dp),
)
```

### Custom engine

You can pass a custom engine to the image API, or configure it via CompositionLocal. The parameter
version has higher priority.

```kotlin
// Android
val engine = GlideRequestEngine(
    requestBuilder = { ctx ->
        GlideApp.with(ctx.context)
            .asDrawable()
    },
)


// iOS
val engine = CoilRequestEngine(
    factory = CoilImageLoaderSingletonFactory(),
)

// parameter passing
AkitAsyncImage(
    model = url,
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    engine = engine,
)

// or CompositionLocal
@Composable
fun ScreenContent(...) = CompositionLocalProvider(
    LocalPlatformAsyncRequestEngine provides engine
) {
    AkitAsyncImage(...)
}
```

```kotlin
@Composable
fun ScreenContent(...) = CompositionLocalProvider(
    LocalPlatformAsyncRequestEngine provides engine
) {
    AkitAsyncImage(...)
}
```

### Large bitmap guard (Android)

Akit includes a size guard to avoid `draw too large bitmap` crashes. Customize the limit via Glide
default options:

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

## Glide extensions (Android)

If the annotation processor does not register the LibraryGlideModules, register them manually:

```kotlin
import com.bumptech.glide.annotation.Excludes

@GlideModule
@Excludes(
    value = [
        NinePatchLibraryGlideModule::class,
        LottieLibraryGlideModule::class,
    ]
)
class GlideAppModuleImpl : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
        LottieLibraryGlideModule().registerComponents(context, glide, registry)
    }
}
```

If `@Excludes` cannot be used, guard with `registerCount` to avoid duplicate registration:

```kotlin
@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        if (NinePatchLibraryGlideModule.registerCount == 0) {
            NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
        }
        if (LottieLibraryGlideModule.registerCount == 0) {
            LottieLibraryGlideModule().registerComponents(context, glide, registry)
        }
    }
}
```

## Compose Multiplatform Resources Plugin

The resources plugin generates a shared `Res` object and platform `ResourceId` from `src/res`:
- Android: access resources via Android `R`, and `ResourceId` is the `R` id.
- iOS: `ResourceId` is a file URL pointing to resources bundled in the framework/app; density
  qualifiers become `@2x`/`@3x` variants.
Full details: [README_RESOURCE.md](README_RESOURCE.md).

### Plugin + runtime

```kotlin

plugins {
    id("cn.szkug.akit.resources") version "lastVersion"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:resources-runtime:$lastVersion")
        }
    }
}

// Configure your resource rules
cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res")) // Resource root, default src/res
    packageName.set("com.example.app") // Package name for Res in common
    androidNamespace.set("com.example.app")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosResourcesPrefix.set("cmp-res") // iOS subdir under compose-resources (default: <ModuleName>Res)
    iosExtraResDir.set(layout.projectDirectory.dir("src/iosMain/res"))
    whitelistEnabled.set(false) // Only allow whitelisted ids when enabled
    stringsWhitelistFile.set(layout.projectDirectory.file("res-whitelist/strings.txt"))
    drawablesWhitelistFile.set(layout.projectDirectory.file("res-whitelist/drawables.txt"))
}
```

Whitelist files are plain text, one resource id per line. Lines starting with `#` or `//` are
ignored.

Extra resource dirs follow the same Android-style layout. They are only added to the
corresponding platform `Res` (no common expect entries). Missing dirs are ignored.

### Usage

Resource example:
res/values/strings.xml -> `<string name="hello">Hello</string>`
- commonMain: `expect Res.strings.hello: ResourceId`
- androidMain: `Res.strings.hello = R.strings.hello`
- iosMain: `Res.strings.hello = NSURL.fileURLWithPath("$resourcesPrefix|hello")`

iOS resources are synced into the app bundle under `compose-resources/<iosResourcesPrefix>` by
`syncCmpResourcesForXcode`, which runs during `embedAndSignAppleFrameworkForXcode` (Xcode build)
and merges transitive module resources (via `cmpComposeResourcesElements`).
If you need a manual output directory (e.g. CLI debugging or framework export), set
`-Pcmp.ios.resources.outputDir=/path/to/Resources`.
When using CocoaPods, resources are synced into
`build/compose/cocoapods/compose-resources` and wired to `syncFramework`.

When calling `stringResource` / `painterResource` from the runtime library, locale handling is
automatic:
- Android: uses `androidx.compose.ui.res.stringResource` and depends on the `Context` locale.
- iOS: reads `AppleLanguages` from `NSUserDefaults.standardUserDefaults` and loads the matching
  `NSBundle`.

Additionally, `painterResource` handles local .9 resources automatically.

```kotlin
Text(text = stringResource(Res.strings.hello))

Image(
    painter = painterResource(Res.drawable.logo),
    contentDescription = null,
)
```

## RenderScript Toolkit (Android)

This is a republished `renderscript-intrinsics-replacement-toolkit` used by the blur extension and
adapted for 16KB page size.

```kotlin
dependencies {
}
```
