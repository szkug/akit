package munchkin.resources.runtime.glide

import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import munchkin.resources.runtime.BinaryAsyncLoadData
import munchkin.resources.runtime.BinaryPayload
import munchkin.resources.runtime.BinarySource
import munchkin.resources.runtime.RuntimeSvgaRequestEngine
import munchkin.resources.runtime.cacheKey

typealias DownloadGlideRequestBuilder = (GlideRuntimeEngineContext) -> RequestBuilder<File>

class GlideRuntimeSvgaRequestEngine(
    val downloadRequestBuilder: DownloadGlideRequestBuilder = DownloadOnlyGlideRequestBuilder,
) : RuntimeSvgaRequestEngine<GlideRuntimeEngineContext>, GlideRuntimeContextRegisterEngine {

    override suspend fun requestSvga(
        engineContext: GlideRuntimeEngineContext,
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
        val Normal = GlideRuntimeSvgaRequestEngine()

        val DownloadOnlyGlideRequestBuilder: DownloadGlideRequestBuilder
            get() = { context ->
                Glide.with(context.androidContext).downloadOnly()
            }

        init {
            GlideRuntimeContextRegisterEngine.register()
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
