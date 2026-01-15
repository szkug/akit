# AKit

Compose Multiplatform 的图片加载、NinePatch 渲染、资源访问辅助库。

当前版本：2.0.0-CMP-11

组件（group `cn.szkug.akit`）：
- `akit-image`：Compose Multiplatform 异步图片加载（Android 使用 Glide，iOS 使用 Coil 3）。
- `akit-graph`：共享图形辅助（NinePatch 解析/绘制）。
- `resources-runtime`：Compose Multiplatform 的 ResourceId 运行时。
- `glide-ninepatch-module`：NinePatch 的 Glide 解码器（仅 Android）。
- `glide-blur-module`：基于 RenderScript toolkit 的 Glide 高斯模糊扩展（仅 Android）。
- `renderscript-toolkit-publish`：RenderScript intrinsics 替代实现（仅 Android）。

Gradle 插件：`cn.szkug.akit.resources`（group `cn.szkug.akit.resources`）。

## Akit Image（Compose Multiplatform）

Akit Image 提供 `AkitAsyncImage` 和 `Modifier.akitAsyncBackground`，它通过
`AsyncRequestEngine` 将平台图片加载器桥接为 Compose `Painter`。

额外支持图片类型：
- .9 图加载
- GIF 动图加载

### 用法

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:akit-image:$lastVersion")
        }
    }
}
```

`AsyncImageContext` 作为图片库的上下文，提供了日志、监听器、扩展开关等配置。

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
    engine = YourCustomEngine // 可以自定义加载引擎，Android 默认使用 Glide，iOS 使用 Coil
)
```

`akitAsyncBackground` 默认 Context 会开启.9 图支持，并会适配 .9 图显示内容边距，若希望布局尺寸 不受 .9 图内边距影响，请设置 `ignoreImagePadding = true`。

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

### 自定义引擎

图片库传入支持自定义引擎参数，也支持使用 CompositionLocal 的方式配置引擎，前者优先级更高

```kotlin
// Android
val engine = GlideRequestEngine(
    requestBuilder = { ctx ->
        GlideApp.with(ctx.context)
            .asDrawable()
            // `BlurBitmapConfigOption` 来自 `glide-blur-module`，该模块依赖 RenderScript toolkit。
            .set(BlurBitmapConfigOption, BlurConfig(radius = 12))
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

### Android 侧大图防护

Akit 内置了尺寸保护以避免 `draw too large bitmap` 崩溃。可通过 Glide 默认选项来自定义限制：

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

## Glide 扩展模块（Android，可选）

如果你需要直接使用 Glide 扩展功能（或未通过 `akit-image` 间接依赖），请显式添加：

```kotlin
dependencies {
    implementation("cn.szkug.akit:glide-ninepatch-module:$akitVersion")
    implementation("cn.szkug.akit:glide-blur-module:$akitVersion")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
}
```

如果注解处理器没有注册 LibraryGlideModules，可手动注册：

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

如果无法使用 `@Excludes`，可以改用 `registerCount` 做防重复注册判断：

```kotlin
@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        if (BlurBitmapLibraryGlideModule.registerCount == 0) {
            BlurBitmapLibraryGlideModule().registerComponents(context, glide, registry)
        }
        if (NinePatchLibraryGlideModule.registerCount == 0) {
            NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
        }
    }
}
```

## Compose Multiplatform 资源插件

resources 插件会将 `src/res` 生成统一的 `Res` 对象与平台 `ResourceId`：
- Android：以 Android R 的方式引入资源，`ResourceId` 即 `R` id。
- iOS：`ResourceId` 是指向 framework/app 内资源的文件 URL，密度限定符会转成 `@2x`/`@3x` 变体。

### 插件 + 运行时

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

// 配置你的处理规则
cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res")) // 资源路径，默认 src/res
    packageName.set("com.example.app") // common Res 文件包名
    androidNamespace.set("com.example.app")
    androidExtraResDir.set(layout.projectDirectory.dir("src/android-res"))
    iosResourcesPrefix.set("cmp-res") // iOS 资源 bundle 路径
    iosFrameworkName.set("MyFramework")
    iosFrameworkBundleId.set("com.example.app")
    iosExtraResDir.set(layout.projectDirectory.dir("src/ios-res"))
    whitelistEnabled.set(false) // 开启后仅保留白名单资源
    stringsWhitelistFile.set(layout.projectDirectory.file("res-whitelist/strings.txt"))
    drawablesWhitelistFile.set(layout.projectDirectory.file("res-whitelist/drawables.txt"))
}
```

白名单文件为纯文本，一行一个资源 id；以 `#` 或 `//` 开头的行会被忽略。

额外资源目录使用与主资源相同的目录结构，只会生成对应平台的 Res 字段，目录不存在会被忽略。

### 使用

资源例子：
res/values/strings.xml -> `<string name="hello">Hello</string>`
- commonMain: `expect Res.strings.hello: ResourceId`
- androidMain: `Res.strings.hello = R.strings.hello`
- iosMain: `Res.strings.hello = NSURL.fileURLWithPath("$frameworkName|$resourcesPrefix|hello")`

使用 runtime 库的 `stringResource` / `painterResource` 时会根据 App 语言自动处理多语言：
- Android 侧为 `androidx.compose.ui.res.stringResource` 实现，依赖 `android.content.Context` 的语言配置
- iOS 侧根据 `NSUserDefaults.standardUserDefaults` 的 `AppleLanguages` 来识别当前语言，并获取对应的 NSBundle

此外 `painterResource` 还会自动处理本地的 .9 资源

```kotlin
Text(text = stringResource(Res.strings.hello))

Image(
    painter = painterResource(Res.drawable.logo),
    contentDescription = null,
)
```

## RenderScript 工具包（Android）

这是 `renderscript-intrinsics-replacement-toolkit` 的重新发布版本，供模糊扩展使用，适配了 16KB page size

```kotlin
dependencies {
    implementation("cn.szkug.akit:renderscript-toolkit-publish:$akitVersion")
}
```
