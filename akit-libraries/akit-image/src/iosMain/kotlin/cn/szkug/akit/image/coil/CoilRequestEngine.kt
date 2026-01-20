package cn.szkug.akit.image.coil

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import cn.szkug.akit.image.AsyncLoadData
import cn.szkug.akit.image.AsyncLoadResult
import cn.szkug.akit.image.AsyncImageContext
import cn.szkug.akit.image.AsyncRequestEngine
import cn.szkug.akit.image.RequestModel
import cn.szkug.akit.image.ResolvableImageSize
import cn.szkug.akit.image.ResourceModel
import cn.szkug.akit.lottie.LottieResource
import cn.szkug.akit.resources.runtime.ResourceId
import cn.szkug.akit.resources.runtime.resolveResourcePath
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.asPainter
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.target.Target
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PainterAsyncLoadData(val painter: Painter) : AsyncLoadData {
    override fun painter(): Painter {
        return painter
    }
}


interface CoilImageLoaderFactory {

    fun get(context: AsyncImageContext): ImageLoader
}

private val NinePatchFactory = NinePatchDecoder.Factory()
private val GifFactory = GifDecoder.Factory()
private val LottieFactory = LottieDecoder.Factory()

open class CoilImageLoaderSingletonFactory : CoilImageLoaderFactory {

    private val reference = atomic<ImageLoader?>(null)

    final override fun get(context: AsyncImageContext): ImageLoader {
        return reference.value ?: create(
            context,
            listOf(NinePatchFactory, GifFactory, LottieFactory)
        ).also {
            reference.value = it
        }
    }

    open fun create(
        context: AsyncImageContext,
        internalFactories: List<Decoder.Factory>
    ): ImageLoader {
        return SingletonImageLoader.get(context = context.context)
            .newBuilder().components {
                for (factory in internalFactories) add(factory)
            }.build()
    }
}

private val NormalCoilImageLoaderFactory = CoilImageLoaderSingletonFactory()

class CoilRequestEngine(
    private val factory: CoilImageLoaderFactory = NormalCoilImageLoaderFactory,
) : AsyncRequestEngine<PainterAsyncLoadData> {

    override suspend fun flowRequest(
        context: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<PainterAsyncLoadData>> = callbackFlow {

        val size = size.awaitSize()
        val rawModel = requestModel.model
        val resolvedModel = when (rawModel) {
            is LottieResource -> resolveLottieResource(rawModel.resource)
            is ResourceId -> resolveResourcePath(rawModel)
                ?: error("ResourceId not found: $rawModel")
            else -> rawModel
        }

        val builder = when (resolvedModel) {
            is ImageRequest -> resolvedModel.newBuilder(context.context)
            else -> ImageRequest.Builder(context.context)
                .data(resolvedModel)
        }

        if (context.supportNinepatch) {
            builder.extras[NinePatchDecodeEnabled] = true
        }
        if (rawModel is LottieResource) {
            builder.extras[LottieDecodeEnabled] = true
            builder.extras[LottieIterationsKey] = context.animationIterations
        }

        val target = CoilFlowTarget(context, this)

        val request = builder
            .size(size.width, size.height)
            .listener(target)
            .target(target)
            .build()

        val disposable = factory.get(context).enqueue(request)

        awaitClose { disposable.dispose() }
    }

    companion object {
        val Normal = CoilRequestEngine()
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
    private val context: AsyncImageContext,
    private val scope: ProducerScope<AsyncLoadResult<PainterAsyncLoadData>>,
) : Target, ImageRequest.Listener {

    override fun onStart(request: ImageRequest) {
        context.listener?.onStart(request.data)
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        context.listener?.onSuccess(request.data)
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        val error = result.throwable
        context.listener?.onFailure(request.data, error)
        context.logger.error(
            "CoilFlowTarget",
            "onError ${request.data}, ${error.stackTraceToString()}"
        )
    }

    override fun onCancel(request: ImageRequest) {
        super.onCancel(request)
        context.listener?.onCancel(request.data)
    }

    override fun onStart(placeholder: Image?) {
        val painter = placeholder?.toAkitPainter(context)
        scope.trySend(AsyncLoadResult.Cleared(painter?.toPainterAsyncLoadData()))
    }

    override fun onError(error: Image?) {
        val painter = error?.toAkitPainter(context)
        scope.trySend(AsyncLoadResult.Error(painter?.toPainterAsyncLoadData()))
    }

    override fun onSuccess(result: Image) {
        val painter = result.toAkitPainter(context)
        scope.trySend(AsyncLoadResult.Success(painter.toPainterAsyncLoadData()))
    }
}

private fun Painter.toPainterAsyncLoadData() = PainterAsyncLoadData(this)

private fun Image.toAkitPainter(context: AsyncImageContext): Painter {
    return when (this) {
        is LottieCoilImage -> toPainter()
        is NinePatchCoilImage -> toPainter()
        is GifCoilImage -> toPainter()
        else -> asPainter(context.context)
    }
}
