package cn.szkug.akit.image.coil.support

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.asImage
import coil3.request.Options
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

internal actual val BitmapImage.ninePatchChunk: ByteArray? get() = null

internal actual val BitmapImage.asComposeImageBimap: ImageBitmap get() = bitmap.asComposeImageBitmap()


actual fun decodeBitmap(bytes: ByteArray): BitmapImage? {
    if (bytes.isEmpty()) return null
    val image = SkiaImage.makeFromEncoded(bytes)
    try {
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(image.width, image.height)
        Canvas(bitmap).use { canvas ->
            canvas.drawImageRect(
                image = image,
                src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                dst = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
            )
        }
        return bitmap.asImage()
    } finally {
        image.close()
    }
}

actual fun scaleImage(image: BitmapImage, options: Options): BitmapImage {
    val srcWidth = image.width
    val srcHeight = image.height
    val (outWidth, outHeight) = computeOutputSize(srcWidth, srcHeight, options)
    if (outWidth == srcWidth && outHeight == srcHeight) return image
    val scaled = Bitmap()
    scaled.allocN32Pixels(outWidth, outHeight)
    val image = SkiaImage.makeFromBitmap(image.bitmap)
    try {
        Canvas(scaled).use { canvas ->
            canvas.drawImageRect(
                image = image,
                src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
                dst = Rect.makeWH(outWidth.toFloat(), outHeight.toFloat()),
            )
        }
    } finally {
        image.close()
    }
    return scaled.asImage()
}

actual fun cropNinePatchContent(image: BitmapImage): BitmapImage {
    val contentWidth = (image.width - 2).coerceAtLeast(1)
    val contentHeight = (image.height - 2).coerceAtLeast(1)
    val out = Bitmap().apply { allocN32Pixels(contentWidth, contentHeight) }
    val new = SkiaImage.makeFromBitmap(image.bitmap)
    try {
        Canvas(out).use { canvas ->
            canvas.drawImageRect(
                image = new,
                src = Rect.makeLTRB(
                    1f,
                    1f,
                    (image.width - 1).toFloat(),
                    (image.height - 1).toFloat(),
                ),
                dst = Rect.makeWH(contentWidth.toFloat(), contentHeight.toFloat()),
            )
        }
    } finally {
        new.close()
    }
    out.setImmutable()
    return out.asImage()
}