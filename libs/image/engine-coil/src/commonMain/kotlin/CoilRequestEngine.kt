package munchkin.image.coil

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import munchkin.image.AsyncLoadData
import munchkin.image.AsyncLoadResult
import munchkin.image.AsyncImageContext
import munchkin.image.AsyncRequestEngine
import munchkin.image.RequestModel
import munchkin.image.ResolvableImageSize
import munchkin.image.ResourceModel
import munchkin.image.clampTo
import munchkin.graph.lottie.LottieResource
import munchkin.image.EngineContext
import munchkin.image.EngineContextProvider
import munchkin.image.LocalEngineContextRegister
import munchkin.image.coil.support.GaussianBlurTransformation
import munchkin.image.coil.support.GifDecoder
import munchkin.resources.runtime.ResourceId
import munchkin.resources.runtime.resolveResourcePath
import munchkin.image.coil.support.LottieDecodeEnabled
import munchkin.image.coil.support.LottieDecoder
import munchkin.image.coil.support.LottieIterationsKey
import munchkin.image.coil.support.NinePatchDecodeEnabled
import munchkin.image.coil.support.NinePatchDecoder
import munchkin.image.coil.support.platformDecoderFactories
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.target.Target
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.jvm.JvmInline

@JvmInline
private value class CoilEngineContext(val context: PlatformContext) : EngineContext

private val CoilEngineContextProvider: EngineContextProvider =
    { CoilEngineContext(LocalPlatformContext.current) }
val EngineContext.context get() = (this as CoilEngineContext).context

class PainterAsyncLoadData(val painter: Painter) : AsyncLoadData {
    override fun painter(): Painter {
        return painter
    }
}

interface CoilImageLoaderFactory {

    fun get(context: PlatformContext): ImageLoader
}

open class CoilImageLoaderSingletonFactory : CoilImageLoaderFactory {

    private val reference = atomic<ImageLoader?>(null)

    final override fun get(context: PlatformContext): ImageLoader {
        return reference.value ?: create(
            context,
            listOf(NinePatchDecoder.Factory(), GifDecoder.Factory(), LottieDecoder.Factory()) +
                platformDecoderFactories()
        ).also {
            reference.value = it
        }
    }

    open fun create(
        context: PlatformContext,
        internalFactories: List<Decoder.Factory>
    ): ImageLoader {
        return SingletonImageLoader.get(context = context)
            .newBuilder().components {
                for (factory in internalFactories) add(factory)
            }.build()
    }
}

private val NormalCoilImageLoaderFactory = CoilImageLoaderSingletonFactory()

class CoilRequestEngine(
    private val factory: CoilImageLoaderFactory = NormalCoilImageLoaderFactory,
) : AsyncRequestEngine<PainterAsyncLoadData> {
    override val engineSizeOriginal: Int = -1

    override suspend fun flowRequest(
        engineContext: EngineContext,
        imageContext: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<PainterAsyncLoadData>> = callbackFlow {

        val requestedSize = size.awaitSize()
        val resolvedSize = requestedSize.clampTo(imageContext.sizeLimit)
        if (requestedSize != resolvedSize && imageContext.sizeLimit != null) {
            imageContext.logger.warn(
                "CoilRequestEngine",
                "clamp request size: $requestedSize -> $resolvedSize by limit=${imageContext.sizeLimit}"
            )
        }
        val rawModel = requestModel.model
        val resolvedModel = when (rawModel) {
            is LottieResource -> resolveLottieResource(rawModel.resource)
            is ResourceId -> resolveResourcePath(rawModel)
                ?: error("ResourceId not found: $rawModel")

            else -> rawModel
        }

        val builder = when (resolvedModel) {
            is ImageRequest -> resolvedModel.newBuilder(engineContext.context)
            else -> ImageRequest.Builder(engineContext.context)
                .data(resolvedModel)
        }

        if (imageContext.supportNinepatch) {
            builder.extras[NinePatchDecodeEnabled] = true
        }
        if (rawModel is LottieResource) {
            builder.extras[LottieDecodeEnabled] = true
            builder.extras[LottieIterationsKey] = imageContext.animationIterations
        }
        val blurConfig = imageContext.blurConfig
        if (blurConfig != null) {
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
        val Normal = CoilRequestEngine()

        init {
            LocalEngineContextRegister.register(CoilRequestEngine::class, CoilEngineContextProvider)
        }
    }
}

private fun resolveLottieResource(resource: Any): Any {
    return when (resource) {
        is ResourceId -> resolveResourcePath(resource) ?: resource
        is String -> resource
        else -> resource
    }
}


private class CoilFlowTarget(
    private val imageContext: AsyncImageContext,
    private val engineContext: EngineContext,
    private val scope: ProducerScope<AsyncLoadResult<PainterAsyncLoadData>>,
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
            "onError ${request.data}, ${error.stackTraceToString()}"
        )
    }

    override fun onCancel(request: ImageRequest) {
        super.onCancel(request)
        imageContext.listener?.onCancel(request.data)
    }

    override fun onStart(placeholder: Image?) {
        val painter = placeholder?.toMunchkinPainter(engineContext)
        scope.trySend(AsyncLoadResult.Cleared(painter?.toPainterAsyncLoadData()))
    }

    override fun onError(error: Image?) {
        val painter = error?.toMunchkinPainter(engineContext)
        scope.trySend(AsyncLoadResult.Error(painter?.toPainterAsyncLoadData()))
    }

    override fun onSuccess(result: Image) {
        val painter = result.toMunchkinPainter(engineContext)
        scope.trySend(AsyncLoadResult.Success(painter.toPainterAsyncLoadData()))
    }
}

private fun Painter.toPainterAsyncLoadData() = PainterAsyncLoadData(this)

private fun Image.toMunchkinPainter(context: EngineContext): Painter {
    return when (this) {
        is LottieCoilImage -> toPainter()
        is NinePatchCoilImage -> toPainter()
        is GifCoilImage -> toPainter()
        else -> asPainter(context.context)
    }
}
