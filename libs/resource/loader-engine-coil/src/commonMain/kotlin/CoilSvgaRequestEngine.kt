package munchkin.resources.loader.coil

import androidx.compose.runtime.Composable
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import munchkin.resources.loader.BinaryAsyncLoadData
import munchkin.resources.loader.BinaryPayload
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.EngineContextProvider
import munchkin.resources.loader.LocalEngineContextRegister
import munchkin.resources.loader.SvgaAsyncRequestEngine
import munchkin.resources.loader.cacheKey
import okio.use
import kotlin.jvm.JvmInline

@JvmInline
private value class CoilSvgaEngineContext(val context: PlatformContext) : EngineContext

private val CoilSvgaEngineContextProvider: EngineContextProvider =
    { CoilSvgaEngineContext(LocalPlatformContext.current) }

private val EngineContext.platformContext: PlatformContext
    get() = (this as CoilSvgaEngineContext).context

interface CoilImageLoaderFactory {
    fun get(context: PlatformContext): ImageLoader
}

open class CoilImageLoaderSingletonFactory : CoilImageLoaderFactory {

    private val reference = atomic<ImageLoader?>(null)

    final override fun get(context: PlatformContext): ImageLoader {
        return reference.value ?: create(context, internalFactories()).also {
            reference.value = it
        }
    }

    protected open fun internalFactories(): List<Decoder.Factory> = emptyList()

    open fun create(
        context: PlatformContext,
        internalFactories: List<Decoder.Factory>,
    ): ImageLoader {
        return SingletonImageLoader.get(context = context)
            .newBuilder().components {
                for (factory in internalFactories) add(factory)
            }.build()
    }
}

private val NormalCoilBinaryImageLoaderFactory = object : CoilImageLoaderSingletonFactory() {
    override fun internalFactories(): List<Decoder.Factory> = listOf(BinaryPayloadDecoder.Factory())
}

class CoilSvgaRequestEngine(
    val factory: CoilImageLoaderFactory = NormalCoilBinaryImageLoaderFactory,
) : SvgaAsyncRequestEngine {

    override suspend fun requestSvga(
        engineContext: EngineContext,
        source: BinarySource,
    ): BinaryAsyncLoadData {
        return CoilBinarySourceRequester(engineContext.platformContext, factory).request(source)
    }

    companion object {
        val Normal = CoilSvgaRequestEngine()

        init {
            LocalEngineContextRegister.register(
                CoilSvgaRequestEngine::class,
                CoilSvgaEngineContextProvider,
            )
        }
    }
}

private class CoilBinarySourceRequester(
    private val context: PlatformContext,
    private val factory: CoilImageLoaderFactory,
) {

    suspend fun request(source: BinarySource): BinaryAsyncLoadData = withContext(Dispatchers.Default) {
        if (source is BinarySource.Bytes) {
            return@withContext BinaryAsyncLoadData(BinaryPayload(source.value, source.cacheKey, source))
        }
        val request = ImageRequest.Builder(context)
            .data(source.resolveCoilBinaryData())
            .apply {
                extras[BinaryPayloadDecodeEnabled] = true
                if (source is BinarySource.Url && source.headers.isNotEmpty()) {
                    httpHeaders(
                        NetworkHeaders.Builder().apply {
                            source.headers.forEach { (key, value) -> set(key, value) }
                        }.build(),
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

internal expect fun BinarySource.resolveCoilBinaryData(): Any

private class BinaryPayloadCoilImage(
    val bytes: ByteArray,
) : Image {
    override val width: Int = 1
    override val height: Int = 1
    override val size: Long = bytes.size.toLong()
    override val shareable: Boolean = false

    override fun draw(canvas: coil3.Canvas) = Unit
}

private val BinaryPayloadDecodeEnabled = coil3.Extras.Key(false)

private class BinaryPayloadDecoder(
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
