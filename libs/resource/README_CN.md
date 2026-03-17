# Munchkin Resource

用于生成强类型 Compose Multiplatform 资源访问 API 的 Gradle 插件和运行时仓库。

## 模块

- `runtime`：`stringResource`、`colorResource`、`painterResource`、`toDp`、`toSp` 等运行时 API
- `gradle-plugin`：`cn.szkug.munchkin.resources`，负责生成 `Res` 并同步 iOS 资源

## 依赖方式

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

## 能力

- 生成 `Res.strings`、`Res.colors`、`Res.dimens`、`Res.drawable`、`Res.raw`
- 提供 Android/iOS 运行时字符串、复数、颜色、Painter、Dimen 解析能力
- 将 compose 资源同步到 iOS bundle
- 支持基于 KLIB IR 的 iOS 未使用资源裁剪

## 发布

```bash
./gradlew :runtime:publishToMavenLocal :gradle-plugin:publishToMavenLocal
./gradlew :runtime:publishToMavenCentral
./gradlew :runtime:publishAndReleaseToMavenCentral
./gradlew :gradle-plugin:publishPlugins
```

`runtime` 发布到 Maven Central。`gradle-plugin` 发布到 Gradle Plugin Portal，需要提供 `GRADLE_PUBLISH_KEY` 和 `GRADLE_PUBLISH_SECRET`。
