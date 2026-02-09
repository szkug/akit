package cn.szkug.akit.image.coil

import cn.szkug.akit.graph.ninepatch.ImageBitmapNinePatchSource
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchType
import cn.szkug.akit.graph.ninepatch.parseNinePatch
import cn.szkug.akit.image.coil.support.NinePatchDecodeEnabled
import cn.szkug.akit.image.coil.support.NinePatchPlatform
import cn.szkug.akit.image.coil.support.isPngSource
import coil3.ImageLoader
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

        val bitmap = NinePatchPlatform.decodeBitmap(bytes) ?: return null
        val width = bitmap.width
        val height = bitmap.height

        val ninePatch = if (width >= 3 && height >= 3) {
            val imageBitmap = NinePatchPlatform.bitmapToImageBitmap(bitmap)
            val parsed = parseNinePatch(
                ImageBitmapNinePatchSource(imageBitmap),
                NinePatchPlatform.ninePatchChunkBytes(bitmap)
            )
            if (parsed.type == NinePatchType.Raw || parsed.type == NinePatchType.Chunk) {
                val chunk = parsed.chunk ?: NinePatchChunk.createEmptyChunk()
                val contentBitmap = when (parsed.type) {
                    NinePatchType.Raw -> NinePatchPlatform.cropNinePatchBitmap(bitmap)
                    NinePatchType.Chunk -> bitmap
                    else -> null
                }
                contentBitmap?.let {
                    val contentImage = NinePatchPlatform.bitmapToImageBitmap(it)
                    NinePatchCoilImage(
                        image = NinePatchPlatform.bitmapToCoilImage(it),
                        content = contentImage,
                        chunk = chunk,
                    )
                }
            } else {
                null
            }
        } else {
            null
        }

        if (ninePatch != null) {
            return DecodeResult(image = ninePatch, isSampled = false)
        }

        val scaled = NinePatchPlatform.scaleBitmap(bitmap, options)
        val isSampled = scaled.width < width || scaled.height < height
        return DecodeResult(image = NinePatchPlatform.bitmapToCoilImage(scaled), isSampled = isSampled)
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
