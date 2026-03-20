package munchkin.resources.loader.glide

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
import munchkin.resources.loader.AsyncImageContext
import munchkin.resources.loader.AsyncImageSizeLimit
import munchkin.resources.loader.DrawableModel
import munchkin.resources.loader.ImageAsyncLoadData
import munchkin.resources.loader.ImageAsyncLoadResult
import munchkin.resources.loader.ImageAsyncRequestEngine
import munchkin.resources.loader.RequestModel
import munchkin.resources.loader.ResIdModel
import munchkin.resources.loader.ResolvableImageSize
import munchkin.resources.loader.ResourceModel
import munchkin.resources.loader.glide.extensions.lottie.LottieDecodeOptions
import munchkin.resources.loader.glide.extensions.ninepatch.NinepatchEnableOption
import munchkin.resources.loader.glide.transformation.BitmapTransformation
import munchkin.resources.loader.glide.transformation.DrawableTransformation
import munchkin.resources.loader.glide.transformation.GaussianBlurTransformation
import munchkin.resources.loader.glide.transformation.setupTransforms

abstract class GlideAsyncLoadData<T>(
    open val value: T,
) : ImageAsyncLoadData

class DrawableAsyncLoadData(
    override val value: Drawable,
) : GlideAsyncLoadData<Drawable>(value) {
    override fun painter(): Painter = value.toPainter()
}

typealias GlideImageRequestBuilder = (GlideLoaderEngineContext, AsyncImageContext) -> RequestBuilder<Drawable>

private const val GlideSizeOriginal = Target.SIZE_ORIGINAL

class GlideImageRequestEngine(
    val requestBuilder: GlideImageRequestBuilder = NormalGlideRequestBuilder,
    val bitmapTransformations: List<BitmapTransformation> = emptyList(),
    val drawableTransformations: List<DrawableTransformation> = emptyList(),
) : ImageAsyncRequestEngine<GlideLoaderEngineContext, DrawableAsyncLoadData>, GlideContextRegisterEngine {

    override val engineSizeOriginal: Int = GlideSizeOriginal

    override suspend fun flowRequest(
        engineContext: GlideLoaderEngineContext,
        imageContext: AsyncImageContext,
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
        context: AsyncImageContext,
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
        imageContext: AsyncImageContext,
        context: android.content.Context,
        requestModel: RequestModel,
        size: ResolvableImageSize,
        sizeLimit: AsyncImageSizeLimit?,
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
        val Normal = GlideImageRequestEngine()

        val NormalGlideRequestBuilder: GlideImageRequestBuilder
            get() = { context, _ ->
                Glide.with(context.androidContext).asDrawable()
            }

        init {
            GlideContextRegisterEngine.register()
        }
    }
}

private data class LottieInstanceKey(private val resId: Int)
