# Munchkin Cats

Munchkin Compose Multiplatform 组件库的统一接入文档入口。

## 模块说明

- Resource
  把 Android 风格资源生成强类型 `Res.*` 访问器，并在 Android / iOS 上统一读取。
  文档： [docs/README_RESOURCE_CN.md](./docs/README_RESOURCE_CN.md)
- Image
  提供统一异步图片 API，并可选择 Coil / Glide 引擎。
  文档： [docs/README_IMAGE_CN.md](./docs/README_IMAGE_CN.md)
- Graph
  提供 NinePatch、Lottie Painter、模糊能力、阴影绘制。
  文档： [docs/README_GRAPH_CN.md](./docs/README_GRAPH_CN.md)

## 快速接入

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:runtime:<version>")
            implementation("cn.szkug.munchkin:image:<version>")
            implementation("cn.szkug.munchkin:graph:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
            // 或 implementation("cn.szkug.munchkin:engine-glide:<version>")
        }
        iosMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
    }
}

plugins {
    id("cn.szkug.munchkin.resources") version "<version>"
}
```

## 应该先看哪份文档？

- 需要生成 `Res.*`、`stringResource`、`painterResource`、`toDp`、`toSp`？
  先看 [docs/README_RESOURCE_CN.md](./docs/README_RESOURCE_CN.md)
- 需要 `MunchkinAsyncImage`、背景图加载、Glide 或 Coil？
  先看 [docs/README_IMAGE_CN.md](./docs/README_IMAGE_CN.md)
- 需要 NinePatch、`rememberLottiePainter`、`Toolkit` 或 `Modifier.munchkinShadow`？
  先看 [docs/README_GRAPH_CN.md](./docs/README_GRAPH_CN.md)
