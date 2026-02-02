package cn.szkug.akit.image.glide

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import cn.szkug.akit.image.glide.extensions.lottie.LottieDecodeOptions
import cn.szkug.akit.graph.toPainter
import cn.szkug.akit.image.AsyncLoadData
import cn.szkug.akit.image.AsyncLoadResult
import cn.szkug.akit.image.AsyncImageContext
import cn.szkug.akit.image.BitmapTransformation
import cn.szkug.akit.image.DrawableModel
import cn.szkug.akit.image.DrawableRequestEngine
import cn.szkug.akit.image.DrawableTransformation
import cn.szkug.akit.image.RequestModel
import cn.szkug.akit.image.ResIdModel
import cn.szkug.akit.image.ResolvableImageSize
import cn.szkug.akit.image.ResourceModel
import cn.szkug.akit.graph.lottie.LottieResource
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import cn.szkug.akit.image.glide.extensions.ninepatch.NinepatchEnableOption
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.flow.Flow
import java.io.File

class DrawableAsyncLoadData(val drawable: Drawable) : AsyncLoadData {
    override fun painter(): Painter {
        return drawable.toPainter()
    }
}

typealias GlideRequestBuilder = (AsyncImageContext) -> RequestBuilder<Drawable>


class GlideRequestEngine(
    val requestBuilder: GlideRequestBuilder = NormalGlideRequestBuilder,
    val bitmapTransformations: List<BitmapTransformation> = emptyList(),
    val drawableTransformations: List<DrawableTransformation> = emptyList(),
) : DrawableRequestEngine {

    override suspend fun flowRequest(
        context: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<DrawableAsyncLoadData>> {
        val blurConfig = context.blurConfig
        val contextBitmapTransformations = if (blurConfig == null) {
            bitmapTransformations
        } else {
            bitmapTransformations + GaussianBlurTransformation(blurConfig)
        }
        return requestBuilder(context).setupTransforms(
            contentScale,
            contextBitmapTransformations,
            drawableTransformations
        ).setupContext(context, requestModel)
            .setupSize(size)
            .setupFailure(failureModel)
            .flowOfRequest(context, requestModel, size)
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
        return when (val size = size.readySize()) {
            null -> this
            else -> override(size.width, size.height)
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
        context: AsyncImageContext,
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
        }.flowDrawableOfSize(context, size)
    }

    companion object {
        val Normal = GlideRequestEngine()

        val NormalGlideRequestBuilder: GlideRequestBuilder
            get() = { context ->
                Glide.with(context.context).asDrawable()
            }
    }
}

private class LottieInstanceKey(private val resId: Int)
