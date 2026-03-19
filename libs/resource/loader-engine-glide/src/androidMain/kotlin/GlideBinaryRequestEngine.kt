package munchkin.resources.loader.glide

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import munchkin.resources.loader.BinaryAsyncLoadData
import munchkin.resources.loader.BinaryEngineContext
import munchkin.resources.loader.BinaryEngineContextProvider
import munchkin.resources.loader.BinaryPayload
import munchkin.resources.loader.BinaryRequestEngine
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.LocalBinaryEngineContextRegister
import munchkin.resources.loader.cacheKey
import munchkin.resources.runtime.resolveResourcePath
import java.io.File
import kotlin.jvm.JvmInline

@JvmInline
private value class AndroidBinaryEngineContext(val context: Context) : BinaryEngineContext

private val GlideBinaryEngineContextProvider: BinaryEngineContextProvider =
    { AndroidBinaryEngineContext(LocalContext.current) }

val BinaryEngineContext.context get() = (this as AndroidBinaryEngineContext).context

typealias DownloadGlideRequestBuilder = (BinaryEngineContext) -> RequestBuilder<File>

class GlideBinaryRequestEngine(
    val downloadRequestBuilder: DownloadGlideRequestBuilder = DownloadOnlyGlideRequestBuilder,
) : BinaryRequestEngine {

    override suspend fun requestBinary(
        engineContext: BinaryEngineContext,
        source: BinarySource,
    ): BinaryAsyncLoadData = withContext(Dispatchers.IO) {
        if (source is BinarySource.Bytes) {
            return@withContext BinaryAsyncLoadData(BinaryPayload(source.value, source.cacheKey, source))
        }
        val file = downloadRequestBuilder(engineContext)
            .load(source.resolveGlideModel())
            .submit()
            .get()
        BinaryAsyncLoadData(BinaryPayload(file.readBytes(), source.cacheKey(), source))
    }

    companion object {
        val Normal = GlideBinaryRequestEngine()

        val DownloadOnlyGlideRequestBuilder: DownloadGlideRequestBuilder
            get() = { context1 ->
                Glide.with(context1.context).downloadOnly()
            }

        init {
            LocalBinaryEngineContextRegister.register(
                GlideBinaryRequestEngine::class,
                GlideBinaryEngineContextProvider,
            )
        }
    }
}

private fun BinarySource.resolveGlideModel(): Any {
    return when (this) {
        is BinarySource.Bytes -> value
        is BinarySource.FilePath -> File(path)
        is BinarySource.Raw -> {
            val path = resolveResourcePath(id) ?: error("Unable to resolve raw resource: $id")
            File(path)
        }
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
