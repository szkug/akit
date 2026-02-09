package cn.szkug.akit.image

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import cn.szkug.akit.graph.AnimatablePainter
import cn.szkug.akit.graph.EmptyPainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


interface AsyncRequestEngine<Data : AsyncLoadData> {

    companion object {}

    val engineSizeOriginal: Int

    suspend fun flowRequest(
        engineContext: EngineContext,
        imageContext: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<Data>>
}

internal fun AsyncRequestEngine<*>.asAsyncLoadDataEngine(): AsyncRequestEngine<AsyncLoadData> {
    @Suppress("UNCHECKED_CAST")
    return this as AsyncRequestEngine<AsyncLoadData>
}

internal abstract class AsyncRequestNode(
    private val engine: AsyncRequestEngine<AsyncLoadData>,
    protected var requestModel: RequestModel,
    private var placeholderModel: PainterModel?,
    private var failureModel: ResourceModel?,
    imageContext: AsyncImageContext,
    engineContext: EngineContext,
    contentScale: ContentScale,
) : Modifier.Node() {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    protected val size: ResolvableImageSize = AsyncImageSize(engine.engineSizeOriginal)

    protected fun Constraints.inferredSize(): Size {
        val width = if (hasBoundedWidth) maxWidth else engine.engineSizeOriginal
        val height = if (hasBoundedHeight) maxHeight else engine.engineSizeOriginal
        return Size(width.toFloat(), height.toFloat())
    }

    var imageContext: AsyncImageContext = imageContext
        private set
    var engineContext: EngineContext = engineContext
        private set

    var contentScale: ContentScale = contentScale
        private set

    protected val placeablePainter: Painter
        get() = placeholderModel?.painter ?: EmptyPainter

    protected var painter: Painter = placeablePainter
        private set(value) {
            if (field === value) return
            onPainterChanged(field, value)
            field = value
        }

    private var rememberJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    protected open fun onPainterChanged(old: Painter, new: Painter) {
        if (old is AnimatablePainter) old.stopAnimation()
        if (new is AnimatablePainter) new.startAnimation(imageContext.coroutineContext)
    }

    protected open fun onStopRequest() {
        if (!size.sizeReady()) size.putSize(Size.Unspecified)
    }

    protected fun resolvePainter(
        result: AsyncLoadResult<AsyncLoadData>,
        failurePainter: Painter?,
        placeholderPainter: Painter,
    ): Painter {
        return when (result) {
            is AsyncLoadResult.Error -> result.data?.painter() ?: failurePainter
            ?: placeholderPainter

            is AsyncLoadResult.Success -> result.data.painter()
            is AsyncLoadResult.Cleared -> placeholderPainter
        }
    }

    protected open fun onCollectResult(painter: Painter?) {}

    @OptIn(ExperimentalComposeUiApi::class)
    protected fun startRequest(requestModel: RequestModel) =
        sideEffect {

            imageContext.logger.debug("startRequest") { "cur:$requestModel req:$requestModel ${rememberJob?.isActive}" }

            if (this.requestModel != requestModel) return@sideEffect

            if (rememberJob != null) return@sideEffect

            trace("AsyncRequestNode.launch") {
                rememberJob = (coroutineScope + Dispatchers.Main.immediate).launch {

                    val failurePainter = (failureModel as? PainterModel)?.painter
                    val placeholderPainter = placeablePainter

                    val model = requestModel.model
                    if (model is Painter) {
                        painter = model
                        onCollectResult(model)
                    } else engine.flowRequest(
                        engineContext,
                        imageContext,
                        size,
                        contentScale,
                        requestModel,
                        failureModel
                    ).collectLatest { result ->
                        imageContext.logger.debug("collectLatest") { "$requestModel $result" }

                        val nextPainter = resolvePainter(result, failurePainter, placeholderPainter)

                        painter = nextPainter
                        onCollectResult(nextPainter)
                    }
                }
            }
        }

    private fun stopRequest() {
        rememberJob?.cancel()
        rememberJob = null
        onStopRequest()
    }

    /**
     * Setup state and start request.
     *
     * It may be call when node attach or update
     */
    open fun setup() {
        val current = painter
        if (current is AnimatablePainter) current.startAnimation(imageContext.coroutineContext)
        startRequest(requestModel)
    }

    /**
     * Reset all state and stop request.
     *
     * It may be call node detach/reset or model [update]
     */
    open fun reset() {
        stopRequest()
        painter = placeablePainter
    }

    open fun update(
        requestModel: RequestModel,
        placeholderModel: PainterModel?,
        failureModel: ResourceModel?,
        contentScale: ContentScale,
        imageContext: AsyncImageContext,
        engineContext: EngineContext
    ) {
        var hasModify = false
        if (requestModel != this.requestModel) {
            this.requestModel = requestModel
            hasModify = true
        }
        if (placeholderModel != this.placeholderModel) {
            this.placeholderModel = placeholderModel
            hasModify = true
        }
        if (failureModel != this.failureModel) {
            this.failureModel = failureModel
            hasModify = true
        }
        if (contentScale != this.contentScale) {
            this.contentScale = contentScale
            hasModify = true
        }
        if (imageContext != this.imageContext) {
            this.imageContext = imageContext
            hasModify = true
        }
        if (engineContext != this.engineContext) {
            this.engineContext = engineContext
            hasModify = true
        }
        if (!hasModify) return
        imageContext.logger.debug("update") { "${this.requestModel} -> $requestModel" }
        reset()
        setup()
    }

    override fun onAttach() {
        super.onAttach()
        imageContext.logger.debug("attach") { "$requestModel" }
        setup()
    }

    override fun onDetach() {
        super.onDetach()
        imageContext.logger.debug("detach") { "$requestModel" }
        reset()
    }

    override fun onReset() {
        super.onReset()
        imageContext.logger.debug("reset") { "$requestModel" }
        reset()
    }
}
