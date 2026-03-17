# Munchkin Image

统一的 Compose Multiplatform 异步图片加载仓库，包含核心 API 与可替换的引擎实现。

## 模块

- `image`：核心异步图片 API 与 Compose Modifier
- `engine-coil`：Android / iOS 共用的 Coil 3 引擎
- `engine-glide`：Android Glide 引擎

## 依赖方式

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:image:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:engine-glide:<version>")
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
        iosMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
    }
}
```

## 能力

- `MunchkinAsyncImage` 与 `Modifier.munchkinAsyncBackground`
- Glide / Coil 双引擎后端
- NinePatch、GIF、Lottie、视频首帧缩略图
- 可选模糊与请求尺寸限制
- 引擎无关的请求管线

## 发布

```bash
./gradlew :image:publishToMavenLocal :engine-coil:publishToMavenLocal :engine-glide:publishToMavenLocal
./gradlew :image:publishToMavenCentral :engine-coil:publishToMavenCentral :engine-glide:publishToMavenCentral
./gradlew :image:publishAndReleaseToMavenCentral :engine-coil:publishAndReleaseToMavenCentral :engine-glide:publishAndReleaseToMavenCentral
```

发布前需要保证同版本的 `munchkin-graph` 和 `munchkin-resource:runtime` 已经可用。
