# Munchkin Resource

Generate strongly typed Compose Multiplatform resource accessors from Android-style resources.

## What You Get

- generated `Res.strings`, `Res.colors`, `Res.dimens`, `Res.drawable`, and `Res.raw`
- shared runtime APIs such as `stringResource`, `pluralStringResource`, `colorResource`, `painterResource`, `toDp`, and `toSp`
- automatic iOS resource syncing for KMP projects

## Setup

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

android {
    namespace = "sample.resources"
}

cmpResources {
    packageName.set("sample.resources")
}
```

## Resource Layout

Put your shared resources under `src/res`, for example:

- `src/res/values/strings.xml`
- `src/res/values/colors.xml`
- `src/res/values/dimens.xml`
- `src/res/drawable/*`
- `src/res/raw/*`

If you still need Android-only resources, you can keep them in `src/androidMain/res` and expose that directory with `androidExtraResDir`.

## Use The Generated APIs

```kotlin
Text(text = stringResource(Res.strings.title))
Text(text = pluralStringResource(Res.strings.common_files, 3, 3, "Kori"))

Box(
    modifier = Modifier
        .size(Res.dimens.button_width.toDp, Res.dimens.button_height.toDp)
        .background(colorResource(Res.colors.color_accent))
)

Image(
    painter = painterResource(Res.drawable.banner),
    contentDescription = null,
)
```

## iOS Notes

The plugin syncs generated resources into the iOS output automatically. If your project produces multiple frameworks or bundles, set `iosResourcesPrefix` so the resource directory name stays stable.
