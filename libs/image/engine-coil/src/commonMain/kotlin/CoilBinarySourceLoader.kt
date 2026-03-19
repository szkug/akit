package munchkin.image.coil

import munchkin.resources.loader.BinaryAsyncLoadData
import munchkin.resources.loader.BinaryPayload
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.cacheKey
import munchkin.image.coil.support.BinaryPayloadDecodeEnabled
import munchkin.resources.runtime.resolveResourcePath
import coil3.PlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CoilBinarySourceRequester(
    private val context: PlatformContext,
    private val factory: CoilImageLoaderFactory,
) {

    suspend fun request(source: BinarySource): BinaryAsyncLoadData = withContext(Dispatchers.Default) {
        if (source is BinarySource.Bytes) {
            return@withContext BinaryAsyncLoadData(BinaryPayload(source.value, source.cacheKey, source))
        }
        val request = ImageRequest.Builder(context)
            .data(source.resolveCoilData())
            .apply {
                extras[BinaryPayloadDecodeEnabled] = true
                if (source is BinarySource.Url && source.headers.isNotEmpty()) {
                    httpHeaders(
                        NetworkHeaders.Builder().apply {
                            source.headers.forEach { (key, value) -> set(key, value) }
                        }.build()
                    )
                }
            }
            .build()
        when (val result = factory.get(context).execute(request)) {
            is SuccessResult -> {
                val image = result.image as? BinaryPayloadCoilImage
                    ?: error("Unexpected Coil binary result: ${result.image::class}")
                BinaryAsyncLoadData(BinaryPayload(image.bytes, source.cacheKey(), source))
            }

            is ErrorResult -> throw result.throwable
        }
    }
}

private fun BinarySource.resolveCoilData(): Any {
    return when (this) {
        is BinarySource.Bytes -> value
        is BinarySource.FilePath -> path
        is BinarySource.Raw -> resolveResourcePath(id) ?: error("Unable to resolve raw resource: $id")
        is BinarySource.UriPath -> value
        is BinarySource.Url -> value
    }
}
