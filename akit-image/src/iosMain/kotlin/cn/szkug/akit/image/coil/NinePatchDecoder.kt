package cn.szkug.akit.image.coil

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import cn.szkug.akit.graph.ninepatch.ImageBitmapNinePatchSource
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchPainter
import cn.szkug.akit.graph.ninepatch.NinePatchType
import cn.szkug.akit.graph.ninepatch.parseNinePatch
import coil3.BitmapImage
import coil3.Extras
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeUtils
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import okio.BufferedSource
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

internal val NinePatchDecodeEnabled = Extras.Key(false)

internal class NinePatchCoilImage(
    private val image: BitmapImage,
    val content: ImageBitmap,
    val chunk: NinePatchChunk,
) : Image {
    private val painter by lazy { NinePatchPainter(content, chunk) }

    override val size: Long get() = image.size
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val shareable: Boolean get() = image.shareable

    override fun draw(canvas: coil3.Canvas) {
        image.draw(canvas)
    }

    fun toPainter(): Painter = painter
}

internal class NinePatchDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val mimeType: String?,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        if (!options.getExtra(NinePatchDecodeEnabled)) return null
        val bufferedSource = source.source()
        if (!isPngSource(mimeType, bufferedSource)) return null
        val bytes = bufferedSource.use { it.readByteArray() }
        if (bytes.isEmpty()) return null
        val image = SkiaImage.makeFromEncoded(bytes)
        try {
            val ninePatch = if (image.width >= 3 && image.height >= 3) {
                val imageBitmap = image.toComposeImageBitmap()
                val ninePatchSource = ImageBitmapNinePatchSource(imageBitmap)
                val parsed = parseNinePatch(ninePatchSource, null)
                if (parsed.type == NinePatchType.Raw) {
                    val chunk = parsed.chunk ?: NinePatchChunk.createEmptyChunk()
                    val croppedBitmap = cropNinePatch(image)
                    croppedBitmap.setImmutable()
                    val content = croppedBitmap.asComposeImageBitmap()
                    NinePatchCoilImage(
                        image = croppedBitmap.asImage(),
                        content = content,
                        chunk = chunk,
                    )
                } else {
                    null
                }
            } else {
                null
            }

            if (ninePatch != null) {
                return DecodeResult(image = ninePatch, isSampled = false)
            }

            val bitmap = makeBitmapFromImage(image, options)
            bitmap.setImmutable()
            val isSampled = bitmap.width < image.width || bitmap.height < image.height
            return DecodeResult(image = bitmap.asImage(), isSampled = isSampled)
        } finally {
            image.close()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!options.getExtra(NinePatchDecodeEnabled)) return null
            return NinePatchDecoder(result.source, options, result.mimeType)
        }
    }
}

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
private fun makeBitmapFromImage(image: SkiaImage, options: Options): Bitmap {
    val srcWidth = image.width
    val srcHeight = image.height
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

    val bitmap = Bitmap()
    bitmap.allocN32Pixels(outWidth, outHeight)
    Canvas(bitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
            dst = Rect.makeWH(outWidth.toFloat(), outHeight.toFloat()),
        )
    }
    return bitmap
}

private fun isPng(mimeType: String?, bytes: ByteArray): Boolean {
    if (mimeType?.startsWith("image/png", ignoreCase = true) == true) return true
    if (bytes.size < 8) return false
    return bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte() &&
        bytes[4] == 0x0D.toByte() &&
        bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() &&
        bytes[7] == 0x0A.toByte()
}

private fun isPngSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("image/png", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(8)
    } catch (_: Exception) {
        return false
    }
    return isPng(mimeType, header)
}

private fun cropNinePatch(image: SkiaImage): Bitmap {
    val contentWidth = (image.width - 2).coerceAtLeast(1)
    val contentHeight = (image.height - 2).coerceAtLeast(1)
    val bitmap = Bitmap().apply { allocN32Pixels(contentWidth, contentHeight) }
    Canvas(bitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            src = Rect.makeLTRB(
                1f,
                1f,
                (image.width - 1).toFloat(),
                (image.height - 1).toFloat(),
            ),
            dst = Rect.makeWH(contentWidth.toFloat(), contentHeight.toFloat()),
        )
    }
    return bitmap
}
