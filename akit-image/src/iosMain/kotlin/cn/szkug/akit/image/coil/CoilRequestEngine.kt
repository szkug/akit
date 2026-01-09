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
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.compose.asPainter
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.target.Target
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PainterAsyncLoadData(val painter: Painter) : AsyncLoadData {
    override fun painter(): Painter {
        return painter
    }
}


typealias CoilImageLoaderBuilder = (AsyncImageContext) -> ImageLoader
private val NormalCoilImageLoaderBuilder: CoilImageLoaderBuilder
    get() = { context ->
        SingletonImageLoader.get(context = context.context)
    }

private val NinePatchFactory = NinePatchDecoder.Factory()

class CoilRequestEngine(
    private val loader: CoilImageLoaderBuilder = NormalCoilImageLoaderBuilder,
) : AsyncRequestEngine<PainterAsyncLoadData> {

    override suspend fun flowRequest(
        context: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<PainterAsyncLoadData>> = callbackFlow {

        val size = size.awaitSize()

        val builder = when (val model = requestModel.model) {
            is ImageRequest -> model.newBuilder(context.context)
            else -> ImageRequest.Builder(context.context)
                .data(model)
        }

        if (context.supportNinepatch) {
            builder.decoderFactory(NinePatchFactory)
            builder.extras[NinePatchDecodeEnabled] = true
        }

        val target = CoilFlowTarget(context, this)

        val request = builder
            .size(size.width, size.height)
            .listener()
            .listener(target)
            .target(target)
            .build()

        val disposable = loader(context).enqueue(request)

        awaitClose { disposable.dispose() }
    }

    companion object {
        val Normal = CoilRequestEngine()
    }
}


private class CoilFlowTarget(
    private val context: AsyncImageContext,
    private val scope: ProducerScope<AsyncLoadResult<PainterAsyncLoadData>>,
) : Target, ImageRequest.Listener {

    override fun onStart(request: ImageRequest) {
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        val error = result.throwable
        context.logger.error("CoilFlowTarget", "onError ${request.data}, ${error.stackTraceToString()}")
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
        is NinePatchCoilImage -> toPainter()
        else -> asPainter(context.context)
    }
}