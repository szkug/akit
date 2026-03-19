# Munchkin Resource

`munchkin-resource` 会把 Android 风格的 `src/res` 生成统一的 `Res` 对象和强类型资源
id，并提供 Android / iOS 运行时 API。

主要运行时包名：

- `munchkin.resources.runtime`
- `munchkin.resources.loader`，用于 `svga` 等模块复用的二进制资源下载
- `munchkin.resources.loader.coil`
- `munchkin.resources.loader.glide`

## 接入方式

```kotlin
plugins {
    id("cn.szkug.munchkin.resources") version "<version>"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:runtime:<version>")
        }
    }
}
```

如果你还需要统一加载 `.svga`、raw 文件或其它自定义二进制资源，可以再增加：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:loader:<version>")
        }
    }
}
```

如果你希望走带缓存的下载链路，可以继续增加：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:loader-engine-coil:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:loader-engine-glide:<version>")
        }
    }
}
```

## 支持的资源类型

### `src/res/values/*.xml`

`values` 目录下每个 XML 都会生成一个对应的 `Res` 分组：

```text
values/strings.xml -> Res.strings
values/colors.xml  -> Res.colors
values/dimens.xml  -> Res.dimens
```

支持的类型：

- `string` -> `StringResourceId`
- `plurals` -> `PluralStringResourceId`
- `color` -> `ColorResourceId`
- `dimen` -> `DimenResourceId`
- `array` -> `Array<ResourceId>`，可推断时会生成更具体的类型数组

`dimen` 支持的单位：`dp`、`sp`、`px`、`in`、`mm`、`pt`。

### 文件资源

- `drawable/*` -> `PaintableResourceId`
- `raw/*` -> `RawResourceId`

`PaintableResourceId` 可在 Android 和 iOS 上通过 `painterResource` 读取。XML Vector 在 iOS
侧也可解析。

## 生成后的 API

插件会在 `cmpResources.packageName` 指定的包下生成 `Res`：

```kotlin
Text(text = stringResource(Res.strings.app_name))
Text(text = pluralStringResource(Res.strings.common_hours, 2, 2))

Box(
    modifier = Modifier
        .size(Res.dimens.button_width.toDp, Res.dimens.button_height.toDp)
        .background(colorResource(Res.colors.primary))
)

Image(
    painter = painterResource(Res.drawable.logo),
    contentDescription = null,
)
```

## 运行时 API

```kotlin
@Composable
fun stringResource(id: StringResourceId, vararg formatArgs: Any): String

@Composable
fun pluralStringResource(id: PluralStringResourceId, count: Int, vararg formatArgs: Any): String

@Composable
fun colorResource(id: ColorResourceId): Color

@Composable
fun painterResource(id: PaintableResourceId): Painter

@get:Composable
val DimenResourceId.toDp: Dp

@get:Composable
val DimenResourceId.toSp: TextUnit

fun resolveResourcePath(id: ResourceId, localeOverride: String? = null): String?
```

## Binary Loader API

当资源插件 / runtime 还不够用，而你需要一个可跨 Android / iOS 复用的二进制资源来源模型时，
可以使用 `loader`。`svga` 就是基于这层能力读取资源。

```kotlin
BinarySource.Url("https://example.com/demo.svga")
BinarySource.Raw(Res.raw.demo_svga)
BinarySource.FilePath("/path/to/demo.svga")
BinarySource.UriPath("content://...")
BinarySource.Bytes(bytes, cacheKey = "demo")
```

`loader` 本身只定义 source 模型和兜底加载；如果你需要缓存下载，应配合
`CoilBinaryRequestEngine` 或 `GlideBinaryRequestEngine` 这类专用 loader engine 一起使用。

## 插件配置

```kotlin
cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res"))
    packageName.set("munchkin.sample")
    androidNamespace.set("munchkin.sample")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosExtraResDir.set(layout.projectDirectory.dir("src/iosMain/res"))
    iosResourcesPrefix.set("MunchkinSampleRes")
    iosPruneUnused.set(false)
    iosPruneLogEnabled.set(false)
}
```

说明：

- 如果 Android Library 已声明 `namespace`，`androidNamespace` 可以不填。
- `iosResourcesPrefix` 默认按模块名生成 `<ModuleName>Res`。
- 资源拥有者模块需要应用插件；iOS 入口模块也应应用插件，以便同步传递依赖资源。

## iOS 输出与同步

生成后的 iOS 资源会被复制到：

```text
compose-resources/<iosResourcesPrefix>/
```

插件会把 `syncComposeMultiplatformResourceResourcesForXcode` 接到 Xcode 和 CocoaPods 相关流程。
CLI 构建时可通过下面的参数覆盖输出目录：

```bash
./gradlew :your:module:syncComposeMultiplatformResourceResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

如果启用 `iosPruneUnused`，插件会扫描 KLIB IR 使用情况并裁剪未使用的 iOS 资源。
