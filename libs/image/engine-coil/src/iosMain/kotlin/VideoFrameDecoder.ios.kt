@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package munchkin.image.coil.support

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

private const val VIDEO_FRAME_TIME_VALUE = 0L
private const val VIDEO_FRAME_TIME_SCALE = 600

internal class VideoFrameDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val sourcePath = source.file().toString()
        val url = NSURL.fileURLWithPath(sourcePath)
        val asset = AVURLAsset.URLAssetWithURL(url, null) ?: return null
        val generator = AVAssetImageGenerator.assetImageGeneratorWithAsset(asset)
        generator.appliesPreferredTrackTransform = true

        val frame = try {
            generator.copyCGImageAtTime(
                requestedTime = CMTimeMake(VIDEO_FRAME_TIME_VALUE, VIDEO_FRAME_TIME_SCALE),
                actualTime = null,
                error = null
            )
        } catch (_: Exception) {
            null
        } ?: return null

        val imageData = UIImagePNGRepresentation(UIImage.imageWithCGImage(frame)) ?: return null
        val bitmapImage = decodeBitmap(imageData.toByteArray()) ?: return null
        val srcWidth = bitmapImage.width
        val srcHeight = bitmapImage.height
        val scaled = scaleImage(bitmapImage, options)

        return DecodeResult(
            image = scaled,
            isSampled = scaled.width < srcWidth || scaled.height < srcHeight,
        )
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

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = this.bytes ?: return ByteArray(0)
    val buffer = ByteArray(length)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, this.length)
    }
    return buffer
}
