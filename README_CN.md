# Munchkin Cats

Munchkin Cats 是 Munchkin Compose Multiplatform 组件库的示例应用和参考接入工程。

## 从这里开始

当你需要下面这些内容时，优先看这个仓库：

- 在正式接入前先体验组件库能力
- 直接参考 Android / Compose Multiplatform 的可运行示例代码
- 验证资源、多端图片加载、NinePatch、Lottie、模糊、阴影等效果

## 运行 Demo

- `apps/android`：Android 示例应用，包含图片和资源相关 Demo
- `apps/cmp`：Compose Multiplatform 示例应用，可用于 Android 和 iOS

直接用 Android Studio 或 IntelliJ IDEA 打开工程，然后从 IDE 运行对应示例应用即可。

## 按需求选择文档

- [Munchkin Graph](./libs/graph/README_CN.md)：NinePatch、Lottie Painter、模糊能力、阴影绘制
- [Munchkin Image](./libs/image/README_CN.md)：`MunchkinAsyncImage`、背景图加载、Coil、Glide
- [Munchkin Resource](./libs/resource/README_CN.md)：生成式 `Res` 访问器和运行时资源 API

## 你可以从示例中学到什么

- 如何用一套 Compose API 统一加载网络图和本地资源
- 如何在 Android / iOS 上选择 Coil，或在 Android 上选择 Glide
- 如何从 Android 风格资源生成强类型 `Res` 访问器
- 如何在共享 UI 中使用 NinePatch、Lottie、模糊和软阴影

如果你是直接接入某个库，请优先阅读 `libs/graph`、`libs/image` 或 `libs/resource` 下各自的 README。
