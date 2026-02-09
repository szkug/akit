package cn.szkug.akit.image.coil

import cn.szkug.akit.image.coil.support.GifPlatform
import cn.szkug.akit.image.coil.support.isGifSource
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.use

internal data class GifDecodeResult(
    val image: GifCoilImage,
    val isSampled: Boolean,
)

internal class GifDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val mimeType: String?,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val result = GifPlatform.decode(bytes, options)
        return DecodeResult(image = result.image, isSampled = result.isSampled)
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
