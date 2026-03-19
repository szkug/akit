# Munchkin Resource

`munchkin-resource` turns Android-style `src/res` into a generated `Res` object with typed resource
ids and provides runtime APIs for Android and iOS.

Main runtime package:

- `munchkin.resources.runtime`
- `munchkin.resources.loader` for reusable binary downloads shared by `svga` and custom resource-based loaders
- `munchkin.resources.loader.coil`
- `munchkin.resources.loader.glide`

## Install

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

If you need to load arbitrary binary assets such as `.svga`, raw files, or custom downloaded payloads
through the same source model on Android and iOS, add:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:loader:<version>")
        }
    }
}
```

To use cached download engines:

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

## Supported Resources

### `src/res/values/*.xml`

Every XML file under `values` is parsed and becomes a generated `Res` group:

```text
values/strings.xml -> Res.strings
values/colors.xml  -> Res.colors
values/dimens.xml  -> Res.dimens
```

Supported types:

- `string` -> `StringResourceId`
- `plurals` -> `PluralStringResourceId`
- `color` -> `ColorResourceId`
- `dimen` -> `DimenResourceId`
- `array` -> `Array<ResourceId>` or a more specific typed array when possible

Dimen units supported: `dp`, `sp`, `px`, `in`, `mm`, `pt`.

### File Resources

- `drawable/*` -> `PaintableResourceId`
- `raw/*` -> `RawResourceId`

`PaintableResourceId` works with `painterResource` on Android and iOS. XML vector drawables are
also parsed on iOS.

## Generated API

The plugin generates `Res` in the package configured by `cmpResources.packageName`:

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

## Runtime API

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

Use `loader` when the resource plugin/runtime alone is not enough and you need a typed binary source
that can be consumed by modules such as `svga` or your own format decoder.

```kotlin
BinarySource.Url("https://example.com/demo.svga")
BinarySource.Raw(Res.raw.demo_svga)
BinarySource.FilePath("/path/to/demo.svga")
BinarySource.UriPath("content://...")
BinarySource.Bytes(bytes, cacheKey = "demo")
```

`loader` only defines the source model and fallback loading. Cached downloads should go through a
dedicated loader engine such as `CoilBinaryRequestEngine` or `GlideBinaryRequestEngine`.

## Plugin Configuration

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

Notes:

- `androidNamespace` is optional when your Android library already defines `namespace`.
- `iosResourcesPrefix` defaults to a camel-cased `<ModuleName>Res`.
- Resource-owning modules should apply the plugin. The iOS entry module should also apply it so
  transitive resources can be synced into the app bundle.

## iOS Output And Sync

Generated iOS resources are copied into:

```text
compose-resources/<iosResourcesPrefix>/
```

The plugin wires `syncComposeMultiplatformResourceResourcesForXcode` into Xcode and CocoaPods
related flows. For CLI builds you can override the destination directory with:

```bash
./gradlew :your:module:syncComposeMultiplatformResourceResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

If `iosPruneUnused` is enabled, the plugin scans KLIB IR usage and removes unused iOS resources.
