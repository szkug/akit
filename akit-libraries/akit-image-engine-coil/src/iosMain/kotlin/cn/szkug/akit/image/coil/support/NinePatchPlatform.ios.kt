package cn.szkug.akit.image.coil.support

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Bitmap
import coil3.Image
import coil3.asImage
import coil3.request.Options
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

internal actual object NinePatchPlatform {
    actual fun decodeBitmap(bytes: ByteArray): Bitmap? {
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
            return bitmap
        } finally {
            image.close()
        }
    }

    actual fun bitmapToImageBitmap(bitmap: Bitmap): ImageBitmap {
        return bitmap.asComposeImageBitmap()
    }

    actual fun bitmapToCoilImage(bitmap: Bitmap): Image {
        return bitmap.asImage()
    }

    actual fun cropNinePatchBitmap(bitmap: Bitmap): Bitmap {
        val contentWidth = (bitmap.width - 2).coerceAtLeast(1)
        val contentHeight = (bitmap.height - 2).coerceAtLeast(1)
        val out = Bitmap().apply { allocN32Pixels(contentWidth, contentHeight) }
        val image = SkiaImage.makeFromBitmap(bitmap)
        try {
            Canvas(out).use { canvas ->
                canvas.drawImageRect(
                    image = image,
                    src = Rect.makeLTRB(
                        1f,
                        1f,
                        (bitmap.width - 1).toFloat(),
                        (bitmap.height - 1).toFloat(),
                    ),
                    dst = Rect.makeWH(contentWidth.toFloat(), contentHeight.toFloat()),
                )
            }
        } finally {
            image.close()
        }
        return out
    }

    actual fun scaleBitmap(bitmap: Bitmap, options: Options): Bitmap {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val (outWidth, outHeight) = computeOutputSize(srcWidth, srcHeight, options)
        if (outWidth == srcWidth && outHeight == srcHeight) return bitmap
        val scaled = Bitmap()
        scaled.allocN32Pixels(outWidth, outHeight)
        val image = SkiaImage.makeFromBitmap(bitmap)
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
        return scaled
    }

    actual fun ninePatchChunkBytes(bitmap: Bitmap): ByteArray? = null
}
