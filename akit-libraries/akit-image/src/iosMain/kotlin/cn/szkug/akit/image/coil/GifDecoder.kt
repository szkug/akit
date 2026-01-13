package cn.szkug.akit.image.coil

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import okio.BufferedSource
import okio.use
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

private const val GIF_HEADER_SIZE = 6L
private const val DEFAULT_FRAME_DURATION_MS = 100

internal class GifCoilImage(
    private val firstFrame: Bitmap,
    val frames: List<ImageBitmap>,
    val frameDurationsMs: IntArray,
    val repeatCount: Int,
) : Image {
    override val size: Long = frames.sumOf { it.width.toLong() * it.height.toLong() * 4L }
    override val width: Int = frames.firstOrNull()?.width ?: -1
    override val height: Int = frames.firstOrNull()?.height ?: -1
    override val shareable: Boolean = false

    override fun draw(canvas: coil3.Canvas) {
        if (firstFrame.width > 0 && firstFrame.height > 0) {
            canvas.writePixels(firstFrame, 0, 0)
        }
    }

    fun toPainter(): GifPainter {
        return GifPainter(frames, frameDurationsMs, repeatCount)
    }
}

internal class GifDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val mimeType: String?,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
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

            val image = GifCoilImage(
                firstFrame = firstFrameBitmap ?: Bitmap(),
                frames = frames,
                frameDurationsMs = durations,
                repeatCount = codec.repetitionCount,
            )
            return DecodeResult(image = image, isSampled = isSampled)
        } finally {
            codec.close()
            data.close()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isGifSource(result.mimeType, result.source.source().peek())) return null
            return GifDecoder(result.source, options, result.mimeType)
        }
    }
}

private fun isGifSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("image/gif", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(GIF_HEADER_SIZE)
    } catch (_: Exception) {
        return false
    }
    return isGifHeader(header)
}

private fun isGifHeader(bytes: ByteArray): Boolean {
    if (bytes.size < GIF_HEADER_SIZE) return false
    return bytes[0] == 'G'.code.toByte() &&
        bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() &&
        bytes[3] == '8'.code.toByte() &&
        (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
        bytes[5] == 'a'.code.toByte()
}

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
private fun scaleBitmap(bitmap: Bitmap, options: Options): Bitmap {
    val srcWidth = bitmap.width
    val srcHeight = bitmap.height
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
    )
    if (options.precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }
    val outWidth = (multiplier * srcWidth).toInt()
    val outHeight = (multiplier * srcHeight).toInt()
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
