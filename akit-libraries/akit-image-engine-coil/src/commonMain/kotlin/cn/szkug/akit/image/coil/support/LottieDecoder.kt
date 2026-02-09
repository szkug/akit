package cn.szkug.akit.image.coil.support

import cn.szkug.akit.image.coil.LottieCoilImage
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import okio.use

internal data class LottieDecodeResult(
    val image: LottieCoilImage,
    val isSampled: Boolean,
)

internal class LottieDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        if (bytes.isEmpty()) {
            error("Lottie source is empty.")
        }
        val result = decodeLottie(bytes, options)
        return DecodeResult(image = result.image, isSampled = result.isSampled)
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!options.getExtra(LottieDecodeEnabled)) return null
            return LottieDecoder(result.source, options)
        }
    }
}

internal expect fun decodeLottie(bytes: ByteArray, options: Options): LottieDecodeResult