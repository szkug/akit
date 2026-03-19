# Munchkin SVGA

`munchkin-svga` 提供 Android / iOS 双端的 Compose SVGA 播放能力，不依赖官方播放器运行时。

主要包名：

- `munchkin.svga`

## 安装

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:loader:<version>")
            implementation("cn.szkug.munchkin:loader-engine-coil:<version>")
            implementation("cn.szkug.munchkin:svga:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:loader-engine-glide:<version>")
        }
    }
}
```

## 支持的资源来源

`MunchkinSvga` 通过 `BinarySource` 读取资源：

- `BinarySource.Url("https://example.com/demo.svga")`
- `BinarySource.Raw(Res.raw.demo_svga)`
- `BinarySource.FilePath("/path/to/demo.svga")`
- `BinarySource.UriPath("file:///path/to/demo.svga")`
- `BinarySource.Bytes(bytes, cacheKey = "demo")`

## 基础用法

```kotlin
val state = rememberSvgaPlayerState(iterations = -1)

MunchkinSvga(
    source = BinarySource.Url("https://example.com/demo.svga"),
    contentDescription = null,
    state = state,
)
```

如果你希望复用缓存和下载链路，需要传入独立的资源下载 engine：

```kotlin
val loaderEngine = CoilBinaryRequestEngine.Normal

MunchkinSvga(
    source = BinarySource.Url("https://example.com/demo.svga"),
    contentDescription = null,
    loaderEngine = loaderEngine,
)
```

## 动态替换能力

通过 `SvgaDynamicEntity` 可以替换动画中的内容：

```kotlin
val dynamic = rememberSvgaDynamicEntity().apply {
    setDynamicText(
        text = "MUNCHKIN",
        forKey = "title",
        style = TextStyle(fontWeight = FontWeight.Bold),
    )
    setHidden(true, "spark")
    setClickArea("cta") { key ->
        println("clicked: $key")
    }
}

MunchkinSvga(
    source = BinarySource.Raw(Res.raw.demo_svga),
    contentDescription = null,
    dynamicEntity = dynamic,
)
```

支持的动态 API：

- `setDynamicText`
- `setDynamicImage`
- `setDynamicPainter`
- `setDynamicDrawer`
- `setHidden`
- `setClickArea`

当前支持的下载 engine：

- Android / iOS 的 `CoilBinaryRequestEngine`
- Android 的 `GlideBinaryRequestEngine`

## 播放控制

`SvgaPlayerState` 提供播放控制：

- `play()`
- `pause()`
- `stop()`
- `seekToFrame(frame)`
- `updateIterations(value)`

`iterations = -1` 表示无限循环，`iterations = 1` 表示只播放一次。

## 格式支持

解码器支持：

- `.svga` zip 容器
- zlib 压缩的 `movie.binary`
- 原始 `movie.binary`
- 原始 `movie.spec`

内置渲染支持：

- bitmap sprite
- vector shape
- matte mask
- clip path
- 动态文本、图片、Painter、自定义绘制回调
- 音频时间轴播放
