package cn.szkug.akit.image.coil.support

import androidx.compose.ui.graphics.ImageBitmap
import cn.szkug.akit.graph.ninepatch.ImageBitmapNinePatchSource
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchType
import cn.szkug.akit.graph.ninepatch.parseNinePatch
import cn.szkug.akit.image.coil.NinePatchCoilImage
import coil3.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import okio.use

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

        val image = decodeBitmap(bytes) ?: return null
        val width = image.width
        val height = image.height

        val ninePatch = if (width >= 3 && height >= 3) {
            val parsed = parseNinePatch(
                ImageBitmapNinePatchSource(image.asComposeImageBimap),
                image.ninePatchChunk
            )
            when (parsed.type) {
                NinePatchType.Raw -> cropNinePatchContent(image)
                NinePatchType.Chunk -> image
                else -> null
            }?.let {
                val chunk = parsed.chunk ?: NinePatchChunk.createEmptyChunk()
                NinePatchCoilImage(
                    image = it,
                    content = it.asComposeImageBimap,
                    chunk = chunk,
                )
            }
        } else null



        val result = if (ninePatch != null) {
            DecodeResult(image = ninePatch, isSampled = false)
        } else {
            val scaled = scaleImage(image, options)
            val isSampled = scaled.width < width || scaled.height < height
            DecodeResult(image = scaled, isSampled = isSampled)
        }

        return result
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

internal expect val BitmapImage.ninePatchChunk: ByteArray?
internal expect val BitmapImage.asComposeImageBimap: ImageBitmap

internal expect fun decodeBitmap(bytes: ByteArray): BitmapImage?
internal expect fun scaleImage(image: BitmapImage, options: Options): BitmapImage
internal expect fun cropNinePatchContent(image: BitmapImage): BitmapImage
