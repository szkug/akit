package com.korilin.compose.akit.image.glide

import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import com.bumptech.glide.RequestBuilder
import com.korilin.compose.akit.image.publics.AsyncImageContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TRACE_SECTION_NAME = "GlideRequestNode"

internal abstract class GlideRequestNode(
    private var requestModel: GlideRequestModel,
    private var placeholderModel: PainterModel?,
    private var failureModel: ResModel?,
    var contentScale: ContentScale,
    var extension: AsyncImageContext,
) : Modifier.Node() {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    protected fun Constraints.inferredGlideSize(): Size {
        val width = if (hasBoundedWidth) {
            maxWidth
        } else {
            com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
        }
        val height =
            if (hasBoundedHeight) {
                maxHeight
            } else {
                com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
            }
        return Size(width.toFloat(), height.toFloat())
    }

    private inline fun log(subtag: String? = null, crossinline message: () -> String) {
        // skip redundant string concatenation
        if (extension.enableLog) GlideDefaults.logger.info(TRACE_SECTION_NAME) {
            if (subtag == null) message()
            else "[$subtag] ${message()}"
        }
    }

    protected val glideSize: ResolvableGlideSize = AsyncGlideSize()

    private val placeablePainter get() = placeholderModel?.painter ?: EmptyPainter

    protected var painter = placeablePainter
        private set(value) {
            stopAnimation(field)
            field = value
        }

    private var rememberJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    /**
     * Launch via [sideEffect] because [onAttach] is called before [update],
     * and [update] is not always called.
     *
     * That means in [onAttach] if we launch the request, we might restart an old request
     * only to have it immediately replaced by a new request, causing jank.
     * Or if we don't launch the new requests in [onAttach],
     * then [update] might not be called and we won't show the old image.
     *
     * [sideEffect] is called after all changes in the tree, so we can always queue a new request,
     * but drop any for old requests by comparing requests builders.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    protected fun startRequest(requestModel: RequestModel) =
        sideEffect {

            log("startRequest") { "cur:$requestModel req:$requestModel ${rememberJob?.isActive}" }

            // The request changed while our sideEffect was queued, which should also have triggered
            // another sideEffect. Wait for that one instead.
            if (this.requestModel != requestModel) return@sideEffect

            if (rememberJob != null || requestModel !is GlideRequestModel) return@sideEffect

            trace("GlideRequestNode.launch") {
                rememberJob = coroutineScope.launch(Dispatchers.IO) {
                    flowRequest(requestModel)
                }
            }
        }

    private fun <T> RequestBuilder<T>.loadRequestModel(requestModel: GlideRequestModel): RequestBuilder<T> {
        return when (val model = requestModel.model) {
            is Int -> load(model)
            is File -> load(model)
            is Uri -> load(model)
            is String -> load(model)
            else -> load(model)
        }
    }

    private fun <T> RequestBuilder<T>.setupFailure(): RequestBuilder<T> {
        return when (val model = failureModel) {
            is ResModel -> error(model.resId)
            else -> this
        }
    }

    private fun <T> RequestBuilder<T>.setupSize(): RequestBuilder<T> {
        return when (val size = glideSize.readySize()) {
            null -> this
            else -> override(size.width, size.height)
        }
    }

    private suspend fun flowRequest(requestModel: GlideRequestModel) {
        extension.requestBuilder(extension.context)
            .setupSize()
            .setupTransforms(contentScale, extension)
            .setupFailure()
            .loadRequestModel(requestModel)
            .flow(glideSize)
            .collectLatest {
                log("startRequest") { "collectLatest $it" }
                val result = when (it) {
                    is GlideLoadResult.Error -> it.drawable?.toPainter() ?: placeablePainter
                    is GlideLoadResult.Success -> it.drawable.toPainter()
                    is GlideLoadResult.Cleared -> placeablePainter
                }

                withContext(Dispatchers.Main) {
                    painter = result
                    startAnimation(painter)
                    onCollectResult(it)
                }
            }
    }

    abstract fun onCollectResult(result: GlideLoadResult)

    private fun stopRequest() {
        rememberJob?.cancel()
        rememberJob = null
        // finish all flow target
        if (!glideSize.sizeReady()) glideSize.putSize(Size.Unspecified)
    }

    private fun startAnimation(painter: Painter) {
        if (painter is AnimatablePainter) painter.startAnimation()
    }

    private fun stopAnimation(painter: Painter) {
        if (painter is AnimatablePainter) painter.stopAnimation()
    }


    /**
     * Setup state and start request.
     *
     * It may be call when node attach or update
     */
    open fun setup() {
        startAnimation(painter)
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
        requestModel: GlideRequestModel,
        placeholderModel: PainterModel?,
        failureModel: ResModel?
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
        if (!hasModify) return
        // if different model, reset all and restart request
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