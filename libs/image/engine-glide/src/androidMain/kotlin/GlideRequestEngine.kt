package munchkin.image.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import munchkin.image.glide.extensions.lottie.LottieDecodeOptions
import munchkin.graph.toPainter
import munchkin.image.AsyncLoadData
import munchkin.image.AsyncLoadResult
import munchkin.image.AsyncImageContext
import munchkin.image.AsyncImageSizeLimit
import munchkin.image.AsyncRequestEngine
import munchkin.image.DrawableModel
import munchkin.image.RequestModel
import munchkin.image.ResIdModel
import munchkin.image.ResolvableImageSize
import munchkin.image.ResourceModel
import munchkin.graph.lottie.LottieResource
import munchkin.image.EngineContext
import munchkin.image.EngineContextProvider
import munchkin.image.LocalEngineContextRegister
import munchkin.resources.loader.BinaryAsyncLoadData
import munchkin.resources.loader.BinaryPayload
import munchkin.resources.loader.BinaryRequestEngine
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.cacheKey
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import munchkin.image.glide.extensions.ninepatch.NinepatchEnableOption
import munchkin.image.glide.transformation.BitmapTransformation
import munchkin.image.glide.transformation.DrawableTransformation
import munchkin.image.glide.transformation.GaussianBlurTransformation
import munchkin.image.glide.transformation.setupTransforms
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import munchkin.resources.runtime.resolveResourcePath
import java.io.File
import androidx.core.net.toUri

@JvmInline
internal value class AndroidContext(val context: Context) : EngineContext

val EngineContext.context get() = (this as AndroidContext).context

private val GlideEngineContextProvider: EngineContextProvider =
    { AndroidContext(LocalContext.current) }

abstract class GlideAsyncLoadData<T>(
    open val value: T,
) : AsyncLoadData

class DrawableAsyncLoadData(
    override val value: Drawable,
) : GlideAsyncLoadData<Drawable>(value) {
    override fun painter(): Painter = value.toPainter()
}

typealias GlideRequestBuilder = (EngineContext, AsyncImageContext) -> RequestBuilder<Drawable>
typealias DownloadGlideRequestBuilder = (EngineContext) -> RequestBuilder<File>

private const val SDK_SIZE_ORIGINAL = Target.SIZE_ORIGINAL

class GlideRequestEngine(
    val requestBuilder: GlideRequestBuilder = NormalGlideRequestBuilder,
    val downloadRequestBuilder: DownloadGlideRequestBuilder = DownloadOnlyGlideRequestBuilder,
    val bitmapTransformations: List<BitmapTransformation> = emptyList(),
    val drawableTransformations: List<DrawableTransformation> = emptyList(),
) : AsyncRequestEngine<DrawableAsyncLoadData>, BinaryRequestEngine {

    override val engineSizeOriginal: Int = SDK_SIZE_ORIGINAL

    override suspend fun flowRequest(
        engineContext: EngineContext,
        imageContext: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<DrawableAsyncLoadData>> {
        val blurConfig = imageContext.blurConfig
        val contextBitmapTransformations = if (blurConfig == null) {
            bitmapTransformations
        } else {
            bitmapTransformations + GaussianBlurTransformation(blurConfig)
        }
        val sizeLimit = imageContext.sizeLimit
        return requestBuilder(engineContext, imageContext).setupTransforms(
            contentScale,
            contextBitmapTransformations,
            drawableTransformations,
        ).setupContext(imageContext, requestModel)
            .setupSize(size)
            .setupFailure(failureModel)
            .flowOfRequest(imageContext, engineContext.context, requestModel, size, sizeLimit)
    }

    private fun <T> RequestBuilder<T>.setupContext(
        context: AsyncImageContext,
        requestModel: RequestModel,
    ): RequestBuilder<T> {
        val enableLottie = context.supportLottie || requestModel.model is LottieResource
        val lottieIterations = when (val model = requestModel.model) {
            is LottieResource -> model.iterations
            else -> context.animationIterations
        }
        var builder = this.set(NinepatchEnableOption, context.supportNinepatch)
            .set(LottieDecodeOptions.Enabled, enableLottie)
            .set(LottieDecodeOptions.Iterations, lottieIterations)
        val lottieModel = requestModel.model as? LottieResource
        val lottieResId = lottieModel?.resource as? Int
        if (lottieResId != null) {
            val lottieCacheKey = "raw:$lottieResId"
            builder = builder
                .set(LottieDecodeOptions.CacheKey, lottieCacheKey)
                .signature(ObjectKey(LottieInstanceKey(lottieResId)))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
        }
        return builder
    }

    private fun <T> RequestBuilder<T>.setupSize(size: ResolvableImageSize): RequestBuilder<T> {
        return when (val ready = size.readySize()) {
            null -> this
            else -> {
                override(ready.width, ready.height)
            }
        }
    }

    private fun <T> RequestBuilder<T>.setupFailure(failureModel: ResourceModel?): RequestBuilder<T> {
        return when (val model = failureModel) {
            is ResIdModel -> error(model.resId)
            is DrawableModel -> error(model.drawable)
            else -> this
        }
    }


    private fun RequestBuilder<Drawable>.flowOfRequest(
        imageContext: AsyncImageContext,
        context: Context,
        requestModel: RequestModel,
        size: ResolvableImageSize,
        sizeLimit: AsyncImageSizeLimit?,
    ): Flow<AsyncLoadResult<DrawableAsyncLoadData>> {
        return when (val model = requestModel.model) {
            is Int -> return flowOfId(context, model)
            is LottieResource -> load(model)
            is File -> load(model)
            is Uri -> load(model)
            is String -> load(model)
            is Bitmap -> load(model)
            is Drawable -> load(model)
            else -> load(model)
        }.flowDrawableOfSize(imageContext, size, sizeLimit)
    }

    override suspend fun requestBinary(
        engineContext: EngineContext,
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
        val Normal = GlideRequestEngine()

        val NormalGlideRequestBuilder: GlideRequestBuilder
            get() = { context1, _ ->
                Glide.with(context1.context).asDrawable()
            }

        val DownloadOnlyGlideRequestBuilder: DownloadGlideRequestBuilder
            get() = { context1 ->
                Glide.with(context1.context).downloadOnly()
            }

        init {
            LocalEngineContextRegister.register(
                GlideRequestEngine::class,
                GlideEngineContextProvider
            )
        }
    }
}

private data class LottieInstanceKey(private val resId: Int)

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
