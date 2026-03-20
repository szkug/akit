package munchkin.resources.runtime.glide

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import kotlinx.coroutines.flow.Flow
import munchkin.graph.lottie.LottieResource
import munchkin.graph.toPainter
import munchkin.resources.runtime.RuntimeImageRequestContext
import munchkin.resources.runtime.RuntimeImageSizeLimit
import munchkin.resources.runtime.DrawableModel
import munchkin.resources.runtime.ImageAsyncLoadData
import munchkin.resources.runtime.ImageAsyncLoadResult
import munchkin.resources.runtime.RuntimeImageRequestEngine
import munchkin.resources.runtime.RequestModel
import munchkin.resources.runtime.ResIdModel
import munchkin.resources.runtime.ResolvableImageSize
import munchkin.resources.runtime.ResourceModel
import munchkin.resources.runtime.glide.extensions.lottie.LottieDecodeOptions
import munchkin.resources.runtime.glide.extensions.ninepatch.NinepatchEnableOption
import munchkin.resources.runtime.glide.transformation.BitmapTransformation
import munchkin.resources.runtime.glide.transformation.DrawableTransformation
import munchkin.resources.runtime.glide.transformation.GaussianBlurTransformation
import munchkin.resources.runtime.glide.transformation.setupTransforms

abstract class GlideAsyncLoadData<T>(
    open val value: T,
) : ImageAsyncLoadData

class DrawableAsyncLoadData(
    override val value: Drawable,
) : GlideAsyncLoadData<Drawable>(value) {
    override fun painter(): Painter = value.toPainter()
}

typealias GlideImageRequestBuilder = (GlideRuntimeEngineContext, RuntimeImageRequestContext) -> RequestBuilder<Drawable>

private const val GlideSizeOriginal = Target.SIZE_ORIGINAL

class GlideRuntimeImageRequestEngine(
    val requestBuilder: GlideImageRequestBuilder = NormalGlideRequestBuilder,
    val bitmapTransformations: List<BitmapTransformation> = emptyList(),
    val drawableTransformations: List<DrawableTransformation> = emptyList(),
) : RuntimeImageRequestEngine<GlideRuntimeEngineContext, DrawableAsyncLoadData>, GlideRuntimeContextRegisterEngine {

    override val engineSizeOriginal: Int = GlideSizeOriginal

    override suspend fun flowRequest(
        engineContext: GlideRuntimeEngineContext,
        imageContext: RuntimeImageRequestContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<ImageAsyncLoadResult<DrawableAsyncLoadData>> {
        val contextBitmapTransformations = imageContext.blurConfig?.let { blurConfig ->
            bitmapTransformations + GaussianBlurTransformation(blurConfig)
        } ?: bitmapTransformations
        return requestBuilder(engineContext, imageContext)
            .setupTransforms(contentScale, contextBitmapTransformations, drawableTransformations)
            .setupContext(imageContext, requestModel)
            .setupSize(size)
            .setupFailure(failureModel)
            .flowOfRequest(imageContext, engineContext.androidContext, requestModel, size, imageContext.sizeLimit)
    }

    private fun <T> RequestBuilder<T>.setupContext(
        context: RuntimeImageRequestContext,
        requestModel: RequestModel,
    ): RequestBuilder<T> {
        val enableLottie = context.supportLottie || requestModel.model is LottieResource
        val lottieIterations = when (val model = requestModel.model) {
            is LottieResource -> model.iterations
            else -> context.animationIterations
        }
        var builder = this
            .set(NinepatchEnableOption, context.supportNinepatch)
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
            else -> override(ready.width, ready.height)
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
        imageContext: RuntimeImageRequestContext,
        context: android.content.Context,
        requestModel: RequestModel,
        size: ResolvableImageSize,
        sizeLimit: RuntimeImageSizeLimit?,
    ): Flow<ImageAsyncLoadResult<DrawableAsyncLoadData>> {
        val model = requestModel.model
        if (model is Int) {
            return flowOfId(context, model)
        }
        val builder: RequestBuilder<Drawable> = when (model) {
            is LottieResource -> load(model)
            is File -> load(model)
            is Uri -> load(model)
            is String -> load(model)
            is Bitmap -> load(model)
            is Drawable -> load(model)
            else -> load(model)
        }
        return builder.flowDrawableOfSize(imageContext, size, sizeLimit)
    }

    companion object {
        val Normal = GlideRuntimeImageRequestEngine()

        val NormalGlideRequestBuilder: GlideImageRequestBuilder
            get() = { context, _ ->
                Glide.with(context.androidContext).asDrawable()
            }

        init {
            GlideRuntimeContextRegisterEngine.register()
        }
    }
}

private data class LottieInstanceKey(private val resId: Int)
