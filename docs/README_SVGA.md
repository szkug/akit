# Munchkin SVGA

`munchkin-svga` provides SVGA playback for Compose Multiplatform on Android and iOS without depending on the official player runtimes.

Main package:

- `munchkin.svga`

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:svga:<version>")
        }
    }
}
```

## Supported Sources

`MunchkinSvga` reads from `BinarySource`:

- `BinarySource.Url("https://example.com/demo.svga")`
- `BinarySource.Raw(Res.raw.demo_svga)`
- `BinarySource.FilePath("/path/to/demo.svga")`
- `BinarySource.UriPath("file:///path/to/demo.svga")`
- `BinarySource.Bytes(bytes, cacheKey = "demo")`

## Basic Usage

```kotlin
val state = rememberSvgaPlayerState(iterations = -1)

MunchkinSvga(
    source = BinarySource.Url("https://example.com/demo.svga"),
    contentDescription = null,
    state = state,
)
```

## Dynamic Replacement

Use `SvgaDynamicEntity` to override content inside the animation:

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

Available dynamic APIs:

- `setDynamicText`
- `setDynamicImage`
- `setDynamicPainter`
- `setDynamicDrawer`
- `setHidden`
- `setClickArea`

## Playback Control

`SvgaPlayerState` controls playback:

- `play()`
- `pause()`
- `stop()`
- `seekToFrame(frame)`
- `updateIterations(value)`

`iterations = -1` means infinite loop. `iterations = 1` means play once.

## Format Support

The decoder handles:

- `.svga` zip containers
- zlib-compressed `movie.binary`
- raw `movie.binary`
- raw `movie.spec`

Built-in rendering support includes:

- bitmap sprites
- vector shapes
- matte masks
- clip paths
- dynamic text, image, painter, and custom draw callbacks
- audio timeline metadata playback
