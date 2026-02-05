# AKit

Compose Multiplatform 的资源、图片加载与图形辅助库。

当前版本：2.0.0-CMP-21

## 组件

- Resources（插件 + 运行时）：生成 `Res` 与强类型 `ResourceId`，跨平台访问资源。
  详情见：[docs/README_RESOURCE_CN.md](./docs/README_RESOURCE_CN.md)
- Image：异步图片加载，支持 NinePatch、Lottie、GIF 与模糊。
  详情见：[docs/README_IMAGE_CN.md](./docs/README_IMAGE_CN.md)
- Graph：NinePatch 解析/绘制、Lottie Painter、RenderScript Toolkit、阴影 Modifier。
  详情见：[docs/README_GRAPH_CN.md](./docs/README_GRAPH_CN.md)

## 依赖

```kotlin
val akitVersion = "2.0.0-CMP-21"
```

运行时库：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.akit:resources-runtime:$akitVersion")
            implementation("cn.szkug.akit:akit-image:$akitVersion")
            implementation("cn.szkug.akit:akit-graph:$akitVersion")
        }
    }
}
```

资源插件（仅需要资源的模块使用；iOS 入口模块必须应用以同步传递依赖资源）：

```kotlin
plugins {
    id("cn.szkug.akit.resources") version akitVersion
}
```

## 快速示例

```kotlin
Text(text = stringResource(Res.strings.app_name))
Text(text = pluralStringResource(Res.strings.common_hours, 2, 2))
Box(Modifier.background(colorResource(Res.colors.primary)))
Image(painter = painterResource(Res.drawable.logo), contentDescription = null)
```

```kotlin
AkitAsyncImage(
    model = "https://example.com/avatar.png",
    contentDescription = null,
    modifier = Modifier.size(96.dp),
)
```

```kotlin
val lottiePainter = rememberLottiePainter(LottieResource(Res.raw.loading))
Image(painter = lottiePainter, contentDescription = null)
```
