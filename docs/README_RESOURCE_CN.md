# AKit Resources（插件 + 运行时）

Akit Resources 将 Android 风格的 `src/res` 生成统一的 `Res` 对象与强类型
`ResourceId`，并提供 Compose Multiplatform 运行时供 Android/iOS 读取。

## 支持的资源类型

### Values XML（`src/res/values/*.xml`）

`values` 目录下所有 XML 都会被解析（不仅是 `strings.xml`）。每个文件都会生成一个
同名的 `Res` 对象：

```
values/strings.xml -> Res.strings
values/colors.xml  -> Res.colors
values/dimens.xml  -> Res.dimens
```

支持的类型：

- `string` -> `StringResourceId`
- `plurals` -> `PluralStringResourceId`
- `color` -> `ColorResourceId`
- `dimen` -> `DimenResourceId`
- `array` -> `Array<ResourceId>`（可推断时生成更具体的类型）

`dimen` 支持的单位：`dp`、`sp`、`px`、`in`、`mm`、`pt`。

`array` 的 item 必须是 `@type/name` 引用。生成器会跨 `values` 文件解析引用，若 item
类型一致则生成 `Array<T>`，否则退化为 `Array<ResourceId>`。

### 文件资源

- `drawable/*` -> `PaintableResourceId`（PNG/JPG/WebP、XML Vector 等）
- `raw/*` -> `RawResourceId`

`PaintableResourceId` 可在 Android 与 iOS 通过 `painterResource` 读取。iOS 侧支持解析
XML Vector。

## 生成的 API

插件会生成 `Res` 对象（包名由 `cmpResources.packageName` 指定）：

```kotlin
Text(text = stringResource(Res.strings.app_name))
val primary = colorResource(Res.colors.primary)
val width = Res.dimens.button_width.toDp
```

资源按类型强约束：

```kotlin
Res.strings.title        // StringResourceId
Res.colors.primary       // ColorResourceId
Res.dimens.button_width  // DimenResourceId
Res.drawable.logo        // PaintableResourceId
Res.raw.config           // RawResourceId
```

## 运行时 API（`resources-runtime`）

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

`DimenResourceId.toDp` / `toSp` 会根据原始单位自动转换。

## iOS 资源输出

生成的资源会被同步到 app bundle 下：

```
compose-resources/<iosResourcesPrefix>/
```

每个 locale 的输出：

- `Localizable.strings`：`string`
- `Localizable.stringsdict`：`plurals`
- `Colors.strings`：`color`
- `Dimens.strings`：`dimen`

运行时会根据 `AppleLanguages` 解析 `.lproj`，必要时回退 `Base.lproj`。

## Gradle 插件配置

```kotlin
plugins {
    id("cn.szkug.akit.resources") version "2.0.0-CMP-21"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:resources-runtime:2.0.0-CMP-21")
        }
    }
}

cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res"))
    packageName.set("com.example.app")
    androidNamespace.set("com.example.app")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosExtraResDir.set(layout.projectDirectory.dir("src/iosMain/res"))
    iosResourcesPrefix.set("MyModuleRes")
    iosPruneUnused.set(false)
    iosPruneLogEnabled.set(false)
}
```

说明：

- `androidNamespace` 可不填，插件会自动读取 Android Library 的 `namespace`。
- `iosResourcesPrefix` 默认按模块名生成 `<ModuleName>Res`。
- 只有“资源拥有者”模块需要应用插件；iOS 入口模块必须应用插件以同步传递依赖资源。

## iOS 同步与裁剪

插件会将 `syncComposeMultiplatformResourceResourcesForXcode` 接到 Xcode 构建
(`embedAndSignAppleFrameworkForXcode`) 与 CocoaPods 流程 (`syncFramework`) 中。

如需 CLI 输出目录，可使用：

```
./gradlew :your:module:syncComposeMultiplatformResourceResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

`iosPruneUnused` 会扫描 KLIB IR 并裁剪未使用的 iOS 资源。启用 `iosPruneLogEnabled`
可输出扫描与裁剪日志。
