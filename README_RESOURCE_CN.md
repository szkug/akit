# Compose Multiplatform 资源插件（Akit）

本文档说明资源插件的方案、实现细节与 iOS 侧注意事项。

## 概览

插件 `cn.szkug.akit.resources` 会把 Android 风格的 `src/res` 生成统一的 `Res`
对象与平台 `ResourceId`：
- Android：`ResourceId` 对应 Android `R` id。
- iOS：`ResourceId` 是包含 `compose-resources/<prefix>/...` 路径的 `NSURL`。

只有“拥有资源”的模块需要应用插件。纯依赖模块无需配置插件，除非它自己也要生成
`Res`。iOS 入口模块（Xcode 构建或导出 framework 的模块）必须应用插件，用于
聚合并同步所有传递依赖资源。

## 生成产物

每个模块的任务与输出：
- `generateComposeMultiplatformResourceResources`：生成 `commonMain`/`androidMain`/`iosMain` 以及 `iosResources`
  到 `build/generated/compose-resources/code`。
- `prepareComposeMultiplatformResourceComposeResources`：将 iOS 资源打包成
  `build/generated/compose-resources/<iosResourcesPrefix>`。
- `cmpComposeResourcesElements`：发布当前模块的 iOS 资源，供依赖方聚合。

## iOS 运行时行为

iOS 侧 `ResourceId` 实际是带 `prefix|relativePath` 的文件 URL。运行时：
- 解析 prefix 与相对路径；
- 从 `NSBundle.mainBundle` 的 `compose-resources/<prefix>` 下查找；
- 按 locale（`.lproj`）与 scale（`@2x/@3x`）解析；
- 加载 strings、图片与 raw 资源。

实现参考 `akit-libraries/resources-runtime/src/iosMain/.../ResourcesRuntime.ios.kt`。

## 依赖链资源如何合并

插件递归收集 `MainImplementation`/`MainApi` 的工程依赖，并将各模块的
`cmpComposeResourcesElements` 聚合到 `cmpComposeResourcesClasspath`。结果是：
- 每个资源模块只负责发布自己的 iOS 资源；
- 入口模块统一聚合传递依赖资源；
- 中间依赖模块无需配置插件（除非有自己的资源）。

## iOS 同步策略

### Xcode 构建

`syncComposeMultiplatformResourceResourcesForXcode` 会在 `embedAndSignAppleFrameworkForXcode` 时执行，
并将 `compose-resources/<prefix>` 拷贝到 app bundle 的资源目录，路径由
以下环境变量确定：
- `BUILT_PRODUCTS_DIR`
- `UNLOCALIZED_RESOURCES_FOLDER_PATH`

适用于标准 Xcode 构建流程。

### CocoaPods

当启用 `org.jetbrains.kotlin.native.cocoapods` 时：
- 资源同步到 `build/compose/cocoapods/compose-resources`；
- `syncComposeMultiplatformResourceResourcesForXcode` 挂到 `syncFramework`。

与 Compose Multiplatform 的 CocoaPods 资源目录保持一致。

### 手动输出（CLI / framework 导出）

非 Xcode 构建或需要固定输出目录时可使用：

```
./gradlew :your:module:syncComposeMultiplatformResourceResourcesForXcode \
  -Pcmp.ios.resources.outputDir=/path/to/Resources
```

再将 `/path/to/Resources/compose-resources/<prefix>` 复制到最终 app bundle。

## 配置示例

```kotlin
cmpResources {
    resDir.set(layout.projectDirectory.dir("src/res"))
    packageName.set("com.example.app")
    androidNamespace.set("com.example.app")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosResourcesPrefix.set("cmp-res")
    iosExtraResDir.set(layout.projectDirectory.dir("src/iosMain/res"))
    iosPruneUnused.set(false)
    iosPruneLogEnabled.set(false)
}
```

## 注意事项

- 建议每个模块使用唯一的 `iosResourcesPrefix`，避免资源冲突。
- iOS 入口模块必须应用插件，保证 `syncComposeMultiplatformResourceResourcesForXcode` 执行。
- 当 `src/res` 不存在时会使用空目录（由 `prepareComposeMultiplatformResourceEmptyResDir` 创建）。
- 运行时找不到资源时，优先检查：
  - bundle 中是否存在 `compose-resources/<prefix>/...`；
  - 是否实际执行了 `embedAndSignAppleFrameworkForXcode` 或 `syncFramework`；
  - prefix 是否正确配置并一致。
