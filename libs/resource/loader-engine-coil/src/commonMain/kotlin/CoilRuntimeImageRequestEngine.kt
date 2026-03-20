package munchkin.resources.runtime.coil

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.Image
import coil3.compose.asPainter
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.target.Target
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import munchkin.resources.runtime.RuntimeImageRequestContext
import munchkin.resources.runtime.ImageAsyncLoadData
import munchkin.resources.runtime.ImageAsyncLoadResult
import munchkin.resources.runtime.RuntimeImageRequestEngine
import munchkin.resources.runtime.RequestModel
import munchkin.resources.runtime.ResolvableImageSize
import munchkin.resources.runtime.ResourceModel
import munchkin.resources.runtime.clampTo
import munchkin.resources.runtime.coil.support.GaussianBlurTransformation
import munchkin.resources.runtime.coil.support.GifDecoder
import munchkin.resources.runtime.coil.support.LottieDecodeEnabled
import munchkin.resources.runtime.coil.support.LottieDecoder
import munchkin.resources.runtime.coil.support.LottieIterationsKey
import munchkin.resources.runtime.coil.support.NinePatchDecodeEnabled
import munchkin.resources.runtime.coil.support.NinePatchDecoder
import munchkin.resources.runtime.coil.support.platformDecoderFactories


abstract class CoilAsyncLoadData<T>(
    open val value: T,
) : ImageAsyncLoadData

class PainterAsyncLoadData(
    override val value: Painter,
) : CoilAsyncLoadData<Painter>(value) {
    override fun painter(): Painter = value
}

private val NormalCoilImageLoaderFactory = object : CoilImageLoaderSingletonFactory() {
    override fun internalFactories(): List<Decoder.Factory> {
        return listOf(NinePatchDecoder.Factory(), GifDecoder.Factory(), LottieDecoder.Factory()) +
            platformDecoderFactories()
    }
}

class CoilRuntimeImageRequestEngine(
    val factory: CoilImageLoaderFactory = NormalCoilImageLoaderFactory,
) : RuntimeImageRequestEngine<CoilRuntimeEngineContext, PainterAsyncLoadData>, CoilRuntimeContextRegisterEngine {

    override val engineSizeOriginal: Int = -1

    override suspend fun flowRequest(
        engineContext: CoilRuntimeEngineContext,
        imageContext: RuntimeImageRequestContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<ImageAsyncLoadResult<PainterAsyncLoadData>> = callbackFlow {
        val requestedSize = size.awaitSize()
        val resolvedSize = requestedSize.clampTo(imageContext.sizeLimit)
        if (requestedSize != resolvedSize && imageContext.sizeLimit != null) {
            imageContext.logger.warn(
                "CoilImageEngine",
                "clamp request size: $requestedSize -> $resolvedSize by limit=${imageContext.sizeLimit}",
            )
        }

        val rawModel = requestModel.model
        val builder = when (val resolvedModel = rawModel.resolveCoilImageData()) {
            is ImageRequest -> resolvedModel.newBuilder(engineContext.context)
            else -> ImageRequest.Builder(engineContext.context).data(resolvedModel)
        }

        if (imageContext.supportNinepatch) {
            builder.extras[NinePatchDecodeEnabled] = true
        }
        if (rawModel is munchkin.graph.lottie.LottieResource) {
            builder.extras[LottieDecodeEnabled] = true
            builder.extras[LottieIterationsKey] = imageContext.animationIterations
        }
        imageContext.blurConfig?.let { blurConfig ->
            builder.transformations(GaussianBlurTransformation(blurConfig))
        }

        val target = CoilFlowTarget(imageContext, engineContext, this)
        val request = builder
            .size(resolvedSize.width, resolvedSize.height)
            .listener(target)
            .target(target)
            .build()

        val disposable = factory.get(engineContext.context).enqueue(request)
        awaitClose { disposable.dispose() }
    }

    companion object {
        val Normal = CoilRuntimeImageRequestEngine()

        init {
            CoilRuntimeContextRegisterEngine.register()
        }
    }
}

internal expect fun Any?.resolveCoilImageData(): Any?

private class CoilFlowTarget(
    private val imageContext: RuntimeImageRequestContext,
    private val engineContext: CoilRuntimeEngineContext,
    private val scope: ProducerScope<ImageAsyncLoadResult<PainterAsyncLoadData>>,
) : Target, ImageRequest.Listener {

    override fun onStart(request: ImageRequest) {
        imageContext.listener?.onStart(request.data)
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        imageContext.listener?.onSuccess(request.data)
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        val error = result.throwable
        imageContext.listener?.onFailure(request.data, error)
        imageContext.logger.error(
            "CoilFlowTarget",
            "onError ${request.data}, ${error.stackTraceToString()}",
        )
    }

    override fun onCancel(request: ImageRequest) {
        super.onCancel(request)
        imageContext.listener?.onCancel(request.data)
    }

    override fun onStart(placeholder: Image?) {
        val painter = placeholder?.toMunchkinPainter(engineContext)
        scope.trySend(ImageAsyncLoadResult.Cleared(painter?.toPainterAsyncLoadData()))
    }

    override fun onError(error: Image?) {
        val painter = error?.toMunchkinPainter(engineContext)
        scope.trySend(ImageAsyncLoadResult.Error(painter?.toPainterAsyncLoadData()))
    }

    override fun onSuccess(result: Image) {
        val painter = result.toMunchkinPainter(engineContext)
        scope.trySend(ImageAsyncLoadResult.Success(painter.toPainterAsyncLoadData()))
    }
}

private fun Painter.toPainterAsyncLoadData() = PainterAsyncLoadData(this)

private fun Image.toMunchkinPainter(context: CoilRuntimeEngineContext): Painter {
    return when (this) {
        is LottieCoilImage -> toPainter()
        is NinePatchCoilImage -> toPainter()
        is GifCoilImage -> toPainter()
        else -> asPainter(context.context)
    }
}
