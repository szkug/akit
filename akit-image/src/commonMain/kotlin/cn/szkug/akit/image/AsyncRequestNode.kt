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

    suspend fun flowRequest(
        context: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<AsyncLoadResult<Data>>
}

internal abstract class AsyncRequestNode<Data : AsyncLoadData>(
    private val engine: AsyncRequestEngine<Data>,
    protected var requestModel: RequestModel,
    private var placeholderModel: PainterModel?,
    private var failureModel: ResourceModel?,
    context: AsyncImageContext,
    contentScale: ContentScale,
) : Modifier.Node() {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    protected val size: ResolvableImageSize = AsyncImageSize()

    protected fun Constraints.inferredSize(): Size {
        val width = if (hasBoundedWidth) {
            maxWidth
        } else {
            SDK_SIZE_ORIGINAL
        }
        val height =
            if (hasBoundedHeight) {
                maxHeight
            } else {
                SDK_SIZE_ORIGINAL
            }
        return Size(width.toFloat(), height.toFloat())
    }

    var context: AsyncImageContext = context
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
        if (new is AnimatablePainter) new.startAnimation()
    }

    protected open fun onStopRequest() {
        if (!size.sizeReady()) size.putSize(Size.Unspecified)
    }

    protected fun resolvePainter(
        result: AsyncLoadResult<Data>,
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

            context.logger.debug("startRequest") { "cur:$requestModel req:$requestModel ${rememberJob?.isActive}" }

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
                    } else engine.flowRequest(context, size, contentScale, requestModel, failureModel).collectLatest { result ->
                        context.logger.debug("collectLatest") { "$requestModel $result" }

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
        if (current is AnimatablePainter) current.startAnimation()
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
        context: AsyncImageContext,
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
        if (context != this.context) {
            this.context = context
            hasModify = true
        }
        if (!hasModify) return
        context.logger.debug("update") { "${this.requestModel} -> $requestModel" }
        reset()
        setup()
    }

    override fun onAttach() {
        super.onAttach()
        context.logger.debug("attach") { "$requestModel" }
        setup()
    }

    override fun onDetach() {
        super.onDetach()
        context.logger.debug("detach") { "$requestModel" }
        reset()
    }

    override fun onReset() {
        super.onReset()
        context.logger.debug("reset") { "$requestModel" }
        reset()
    }
}
