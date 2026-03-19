package munchkin.resources.loader.glide

import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import munchkin.resources.loader.BinaryAsyncLoadData
import munchkin.resources.loader.BinaryPayload
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.LocalEngineContextRegister
import munchkin.resources.loader.SvgaAsyncRequestEngine
import munchkin.resources.loader.cacheKey

typealias DownloadGlideRequestBuilder = (EngineContext) -> RequestBuilder<File>

class GlideSvgaRequestEngine(
    val downloadRequestBuilder: DownloadGlideRequestBuilder = DownloadOnlyGlideRequestBuilder,
) : SvgaAsyncRequestEngine {

    override suspend fun requestSvga(
        engineContext: EngineContext,
        source: BinarySource,
    ): BinaryAsyncLoadData = withContext(Dispatchers.IO) {
        if (source is BinarySource.Bytes) {
            return@withContext BinaryAsyncLoadData(BinaryPayload(source.value, source.cacheKey, source))
        }
        if (source is BinarySource.Raw) {
            val bytes = engineContext.androidContext.resources.openRawResource(source.id).use { it.readBytes() }
            return@withContext BinaryAsyncLoadData(BinaryPayload(bytes, source.cacheKey(), source))
        }
        val file = downloadRequestBuilder(engineContext)
            .load(source.resolveGlideModel())
            .submit()
            .get()
        BinaryAsyncLoadData(BinaryPayload(file.readBytes(), source.cacheKey(), source))
    }

    companion object {
        val Normal = GlideSvgaRequestEngine()

        val DownloadOnlyGlideRequestBuilder: DownloadGlideRequestBuilder
            get() = { context ->
                Glide.with(context.androidContext).downloadOnly()
            }

        init {
            LocalEngineContextRegister.register(
                GlideSvgaRequestEngine::class,
                GlideEngineContextProvider,
            )
        }
    }
}

private fun BinarySource.resolveGlideModel(): Any {
    return when (this) {
        is BinarySource.Bytes -> value
        is BinarySource.FilePath -> File(path)
        is BinarySource.Raw -> error("Raw resources are resolved before Glide model conversion.")
        is BinarySource.UriPath -> value.toUri()
        is BinarySource.Url -> {
            if (headers.isEmpty()) {
                value
            } else {
                GlideUrl(
                    value,
                    LazyHeaders.Builder().apply {
                        headers.forEach { (key, headerValue) -> addHeader(key, headerValue) }
                    }.build(),
                )
            }
        }
    }
}
