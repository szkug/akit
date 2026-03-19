package munchkin.image.coil.support

import munchkin.image.coil.BinaryPayloadCoilImage
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import okio.use

internal class BinaryPayloadDecoder(
    private val source: ImageSource,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        return DecodeResult(
            image = BinaryPayloadCoilImage(bytes),
            isSampled = false,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!options.getExtra(BinaryPayloadDecodeEnabled)) return null
            return BinaryPayloadDecoder(result.source)
        }
    }
}
