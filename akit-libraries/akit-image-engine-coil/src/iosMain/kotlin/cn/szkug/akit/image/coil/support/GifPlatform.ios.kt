package cn.szkug.akit.image.coil.support

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import cn.szkug.akit.image.coil.GifCoilImage
import cn.szkug.akit.image.coil.GifDecodeResult
import coil3.asImage
import coil3.request.Options
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

private const val DEFAULT_FRAME_DURATION_MS = 100

internal actual object GifPlatform {
    actual fun decode(bytes: ByteArray, options: Options): GifDecodeResult {
        val data = Data.makeFromBytes(bytes)
        val codec = try {
            Codec.makeFromData(data)
        } catch (exception: Exception) {
            data.close()
            throw exception
        }
        try {
            if (codec.encodedImageFormat != EncodedImageFormat.GIF) {
                error("Unsupported format: ${codec.encodedImageFormat}")
            }
            val frameCount = codec.frameCount
            val frameInfos = codec.framesInfo
            val durations = IntArray(frameCount) { index ->
                val duration = frameInfos.getOrNull(index)?.duration ?: DEFAULT_FRAME_DURATION_MS
                if (duration <= 0) DEFAULT_FRAME_DURATION_MS else duration
            }
            val frames = ArrayList<ImageBitmap>(frameCount)
            var firstFrameBitmap: Bitmap? = null
            var isSampled = false
            val imageInfo = codec.imageInfo

            repeat(frameCount) { index ->
                val bitmap = Bitmap()
                check(bitmap.allocPixels(imageInfo)) { "allocPixels($imageInfo) failed" }
                codec.readPixels(bitmap, index)
                val scaled = scaleBitmap(bitmap, options)
                if (scaled !== bitmap) {
                    bitmap.close()
                }
                scaled.setImmutable()
                if (firstFrameBitmap == null) firstFrameBitmap = scaled
                isSampled = isSampled || scaled.width < imageInfo.width || scaled.height < imageInfo.height
                frames += scaled.asComposeImageBitmap()
            }

            val painter = GifPainter(frames, durations, codec.repetitionCount)
            val width = frames.firstOrNull()?.width ?: -1
            val height = frames.firstOrNull()?.height ?: -1
            val size = frames.sumOf { it.width.toLong() * it.height.toLong() * 4L }

            val firstFrameImage = firstFrameBitmap?.asImage()
            val image = GifCoilImage(
                firstFrame = firstFrameImage,
                painter = painter,
                width = width,
                height = height,
                size = size,
                shareable = false,
            )

            return GifDecodeResult(image = image, isSampled = isSampled)
        } finally {
            codec.close()
            data.close()
        }
    }
}

private fun scaleBitmap(bitmap: Bitmap, options: Options): Bitmap {
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
