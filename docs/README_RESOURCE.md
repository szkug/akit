# AKit Resources (Gradle plugin + runtime)

AKit Resources turns Android-style `src/res` into a shared `Res` object with typed `ResourceId`
values and provides a Compose Multiplatform runtime to load them on Android and iOS.

## Supported resources

### Values XML (`src/res/values/*.xml`)

Every XML file under `values` is parsed (not only `strings.xml`). A `Res` object is generated per
file name:

```
values/strings.xml -> Res.strings
values/colors.xml  -> Res.colors
values/dimens.xml  -> Res.dimens
```

Supported types:

- `string` -> `StringResourceId`
- `plurals` -> `PluralStringResourceId`
- `color` -> `ColorResourceId`
- `dimen` -> `DimenResourceId`
- `array` -> `Array<ResourceId>` (typed when possible)

Dimen units supported: `dp`, `sp`, `px`, `in`, `mm`, `pt`.

Array items must be `@type/name` references. The generator resolves them across all `values`
files and produces `Array<T>` when all items share the same type; otherwise it falls back to
`Array<ResourceId>`.

### File resources

- `drawable/*` -> `PaintableResourceId` (PNG/JPG/WebP, XML vector, etc.)
- `raw/*` -> `RawResourceId`

`PaintableResourceId` works with `painterResource` on both Android and iOS. XML vector drawables
are parsed on iOS by the runtime.

## Generated API

The plugin generates a `Res` object (package configured by `cmpResources.packageName`):

```kotlin
Text(text = stringResource(Res.strings.app_name))
val primary = colorResource(Res.colors.primary)
val width = Res.dimens.button_width.toDp
```

Resources are strongly typed by category:

```kotlin
Res.strings.title        // StringResourceId
Res.colors.primary       // ColorResourceId
Res.dimens.button_width  // DimenResourceId
Res.drawable.logo        // PaintableResourceId
Res.raw.config           // RawResourceId
```

## Runtime APIs (`resources-runtime`)

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

`DimenResourceId.toDp` / `toSp` honor the original unit and use `Density` conversion when needed.

## iOS resource output

Generated resources are copied into the app bundle under:

```
compose-resources/<iosResourcesPrefix>/
```

Per-locale outputs:

- `Localizable.strings` for `string`
- `Localizable.stringsdict` for `plurals`
- `Colors.strings` for `color`
- `Dimens.strings` for `dimen`

The runtime resolves `*.lproj` by `AppleLanguages` and falls back to `Base.lproj` when needed.

## Gradle plugin setup

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

Notes:

- `androidNamespace` is optional. If it is empty, the plugin reuses the namespace from the
  Android library plugin.
- `iosResourcesPrefix` defaults to a camel-cased `<ModuleName>Res`.
- Only modules that own resources need the plugin. The iOS entry module must apply it so it can
  sync transitive resources into the app bundle.

## iOS sync and pruning

The plugin wires `syncComposeMultiplatformResourceResourcesForXcode` into the Xcode build
(`embedAndSignAppleFrameworkForXcode`) and CocoaPods flow (`syncFramework`). For CLI builds you
can override the output directory with:

```
./gradlew :your:module:syncComposeMultiplatformResourceResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

`iosPruneUnused` removes unused iOS resources by scanning KLIB IR usage. Enable
`iosPruneLogEnabled` to log the scan and pruning results.
