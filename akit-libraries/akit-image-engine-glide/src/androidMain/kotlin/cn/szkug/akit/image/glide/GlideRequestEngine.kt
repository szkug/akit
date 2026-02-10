package cn.szkug.akit.image.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cn.szkug.akit.image.glide.extensions.lottie.LottieDecodeOptions
import cn.szkug.akit.graph.toPainter
import cn.szkug.akit.image.AsyncLoadData
import cn.szkug.akit.image.AsyncLoadResult
import cn.szkug.akit.image.AsyncImageContext
import cn.szkug.akit.image.AsyncRequestEngine
import cn.szkug.akit.image.DrawableModel
import cn.szkug.akit.image.RequestModel
import cn.szkug.akit.image.ResIdModel
import cn.szkug.akit.image.ResolvableImageSize
import cn.szkug.akit.image.ResourceModel
import cn.szkug.akit.graph.lottie.LottieResource
import cn.szkug.akit.image.EngineContext
import cn.szkug.akit.image.EngineContextProvider
import cn.szkug.akit.image.LocalEngineContextRegister
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import cn.szkug.akit.image.glide.extensions.ninepatch.NinepatchEnableOption
import cn.szkug.akit.image.glide.transformation.BitmapTransformation
import cn.szkug.akit.image.glide.transformation.DrawableTransformation
import cn.szkug.akit.image.glide.transformation.GaussianBlurTransformation
import cn.szkug.akit.image.glide.transformation.setupTransforms
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.flow.Flow
import java.io.File

@JvmInline
internal value class AndroidContext(val context: Context) : EngineContext

val EngineContext.context get() = (this as AndroidContext).context

private val GlideEngineContextProvider: EngineContextProvider =
    { AndroidContext(LocalContext.current) }

class DrawableAsyncLoadData(val drawable: Drawable) : AsyncLoadData {
    override fun painter(): Painter {
        return drawable.toPainter()
    }
}

typealias GlideRequestBuilder = (EngineContext, AsyncImageContext) -> RequestBuilder<Drawable>

private const val SDK_SIZE_ORIGINAL = Target.SIZE_ORIGINAL

class GlideRequestEngine(
    val requestBuilder: GlideRequestBuilder = NormalGlideRequestBuilder,
    val bitmapTransformations: List<BitmapTransformation> = emptyList(),
    val drawableTransformations: List<DrawableTransformation> = emptyList(),
) : AsyncRequestEngine<DrawableAsyncLoadData> {

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
        return requestBuilder(engineContext, imageContext).setupTransforms(
            contentScale,
            contextBitmapTransformations,
            drawableTransformations
        ).setupContext(imageContext, requestModel)
            .setupSize(size)
            .setupFailure(failureModel)
            .flowOfRequest(imageContext, engineContext.context, requestModel, size)
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
        }.flowDrawableOfSize(imageContext, size)
    }

    companion object {
        val Normal = GlideRequestEngine()

        val NormalGlideRequestBuilder: GlideRequestBuilder
            get() = { context1, _ ->
                Glide.with(context1.context).asDrawable()
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
