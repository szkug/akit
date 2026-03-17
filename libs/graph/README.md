# Munchkin Graph

Graphics utilities for Compose Multiplatform on Android and iOS.

## When To Use It

Use `munchkin-graph` when your UI needs:

- NinePatch parsing and drawing in shared Compose code
- Lottie files rendered as a `Painter`
- soft shadow rendering with `Modifier.munchkinShadow`
- cross-platform blur and image-processing helpers

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:graph:<version>")
        }
    }
}
```

## NinePatch In Shared UI

```kotlin
val source = ImageBitmapNinePatchSource(imageBitmap)
val parsed = parseNinePatch(source, chunkBytes = null)
val painter = NinePatchPainter(imageBitmap, parsed.chunk ?: NinePatchChunk.createEmptyChunk())

Image(
    painter = painter,
    contentDescription = null,
)
```

## Lottie As A Painter

```kotlin
val painter = rememberLottiePainter(
    LottieResource(Res.raw.loading)
)

Image(
    painter = painter,
    contentDescription = null,
)
```

## Soft Shadow

```kotlin
Box(
    modifier = Modifier
        .size(120.dp)
        .munchkinShadow(
            color = Color.Black.copy(alpha = 0.3f),
            effect = 16.dp,
            offset = DpOffset(0.dp, 6.dp),
        )
)
```

## Platform Notes

- Android uses native toolkit support for blur and pixel processing.
- iOS uses platform-native implementations while keeping the shared API unchanged.
