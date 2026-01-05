package cn.szkug.akit.compose.image

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.trace
import cn.szkug.akit.publics.AsyncImageContext
import cn.szkug.akit.publics.PainterModel
import cn.szkug.akit.publics.ResourceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

internal abstract class AsyncRequestNode<ResultT>(
    private var requestModel: RequestModel,
    private var placeholderModel: PainterModel?,
    private var failureModel: ResourceModel?,
    context: AsyncImageContext,
    contentScale: ContentScale,
) : Modifier.Node() {
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

    protected open fun resolveFailurePainter(): Painter? {
        return (failureModel as? PainterModel)?.painter
    }

    protected fun currentFailureModel(): ResourceModel? = failureModel

    protected open fun onSetup() = Unit

    protected open fun onPainterChanged(old: Painter, new: Painter) = Unit

    protected open fun onStopRequest() = Unit

    protected open fun log(subtag: String? = null, message: () -> String) = Unit

    protected abstract fun flowRequest(requestModel: RequestModel): Flow<ResultT>

    protected abstract fun resolvePainter(
        result: ResultT,
        failurePainter: Painter?,
        placeholderPainter: Painter,
    ): Painter

    protected abstract fun onCollectResult(result: ResultT)

    @OptIn(ExperimentalComposeUiApi::class)
    protected fun startRequest(requestModel: RequestModel) =
        sideEffect {

            log("startRequest") { "cur:$requestModel req:$requestModel ${rememberJob?.isActive}" }

            if (this.requestModel != requestModel) return@sideEffect

            if (rememberJob != null) return@sideEffect

            trace("AsyncRequestNode.launch") {
                rememberJob = (coroutineScope + Dispatchers.Main.immediate).launch {
                    val failurePainter = resolveFailurePainter()
                    val placeholderPainter = placeablePainter
                    flowRequest(requestModel).collectLatest { result ->
                        log("collectLatest") { "$requestModel $result" }
                        val nextPainter = resolvePainter(result, failurePainter, placeholderPainter)
                        painter = nextPainter
                        onCollectResult(result)
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
        onSetup()
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
        log("update") { "${this.requestModel} -> $requestModel" }
        reset()
        setup()
    }

    override fun onAttach() {
        super.onAttach()
        log("attach") { "$requestModel" }
        setup()
    }

    override fun onDetach() {
        super.onDetach()
        log("detach") { "$requestModel" }
        reset()
    }

    override fun onReset() {
        super.onReset()
        log("reset") { "$requestModel" }
        reset()
    }
}
