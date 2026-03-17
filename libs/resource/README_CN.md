# Munchkin Resource

把 Android 风格资源生成为 Compose Multiplatform 可直接使用的强类型资源访问 API。

## 你会得到什么

- 生成的 `Res.strings`、`Res.colors`、`Res.dimens`、`Res.drawable`、`Res.raw`
- 共享运行时 API：`stringResource`、`pluralStringResource`、`colorResource`、`painterResource`、`toDp`、`toSp`
- KMP 工程下自动完成 iOS 资源同步

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

android {
    namespace = "sample.resources"
}

cmpResources {
    packageName.set("sample.resources")
}
```

## 资源目录约定

把共享资源放在 `src/res` 下，例如：

- `src/res/values/strings.xml`
- `src/res/values/colors.xml`
- `src/res/values/dimens.xml`
- `src/res/drawable/*`
- `src/res/raw/*`

如果你还需要 Android 独有资源，可以继续放在 `src/androidMain/res`，并通过 `androidExtraResDir` 暴露给插件。

## 使用生成后的 API

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

## iOS 说明

插件会自动把生成后的资源同步到 iOS 输出目录。如果你的工程会产出多个 framework 或 bundle，建议设置 `iosResourcesPrefix`，保证资源目录名稳定。
