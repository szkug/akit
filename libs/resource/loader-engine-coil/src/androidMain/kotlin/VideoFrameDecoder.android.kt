package munchkin.resources.loader.coil.support

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.graphics.scale
import coil3.annotation.ExperimentalCoilApi
import coil3.ImageLoader
import coil3.asImage
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
import kotlin.math.roundToInt

private const val VIDEO_FRAME_TIME_US = 0L

internal class VideoFrameDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(source.file().toString())
            val frame = retriever.getFrameAtTime(
                VIDEO_FRAME_TIME_US,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return null
            val srcWidth = frame.width
            val srcHeight = frame.height
            val scaled = frame.scaleForRequest(options)
            DecodeResult(
                image = scaled.asImage(),
                isSampled = scaled.width < srcWidth || scaled.height < srcHeight,
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isLikelyVideoSource(result.mimeType, result.source.source().peek())) return null
            return VideoFrameDecoder(result.source, options)
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
private fun Bitmap.scaleForRequest(options: Options): Bitmap {
    val source = this
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = source.width,
        srcHeight = source.height,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )
    var scale = DecodeUtils.computeSizeMultiplier(
        srcWidth = source.width,
        srcHeight = source.height,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
    )
    if (options.precision == Precision.INEXACT) {
        scale = scale.coerceAtMost(1.0)
    }
    if (scale == 1.0) return source

    val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
    if (targetWidth == source.width && targetHeight == source.height) return source

    return source.scale(targetWidth, targetHeight, true).also {
        source.recycle()
    }
}
