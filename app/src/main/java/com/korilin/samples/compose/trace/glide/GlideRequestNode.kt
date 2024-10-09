package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import com.bumptech.glide.RequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TRACE_SECTION_NAME = "GlideRequestNode"

internal abstract class GlideRequestNode(
    var nodeModel: GlideNodeModel,
    var loadingModel: GlidePlaceholderModel?,
    var failureModel: GlidePlaceholderModel?,
    var contentScale: ContentScale,
) : Modifier.Node() {

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

    private inline fun log(subtag: String? = null, message: () -> String) {
        // skip redundant string concatenation
        if (GlidePainterLogger.LOGGER_ENABLE)
            GlidePainterLogger.log(TRACE_SECTION_NAME) {
                if (subtag == null) message()
                else "[$subtag] ${message()}"
            }
    }

    abstract val glideSize: ResolvableGlideSize

    private val placeablePainter = when (val model = nodeModel) {
        is GlideRequestModel -> null
        is PainterModel -> model.painter
    } ?: (loadingModel as? PainterModel)?.painter ?: EmptyPainter

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
    protected fun startRequest(requestModel: GlideNodeModel) =
        sideEffect {

            log("startRequest") { "cur:$nodeModel req:$nodeModel ${rememberJob?.isActive}" }

            // The request changed while our sideEffect was queued, which should also have triggered
            // another sideEffect. Wait for that one instead.
            if (this.nodeModel != requestModel) return@sideEffect

            if (rememberJob != null || requestModel !is GlideRequestModel) return@sideEffect

            trace("GlidePainterNode.launch") {
                rememberJob = coroutineScope.launch(Dispatchers.IO) {
                    flowRequest(requestModel)
                }
            }
        }

    private fun RequestBuilder<Drawable>.setupScaleTransform(): RequestBuilder<Drawable> {
        return when (contentScale) {
            ContentScale.Crop -> optionalCenterCrop()

            // Outside compose, glide would use fitCenter() for FIT. But that's probably not a good
            // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
            // So instead we'll do something different and prefer not to upscale, which means using
            // centerInside(). The UI can still scale the view even if the Bitmap is smaller.
            ContentScale.Fit,
            ContentScale.FillHeight,
            ContentScale.FillWidth,
            ContentScale.FillBounds -> optionalCenterInside()

            ContentScale.Inside -> optionalCenterInside()

            // NONE
            else -> this
        }
    }

    private fun RequestBuilder<Drawable>.loadRequestModel(requestModel: GlideRequestModel): RequestBuilder<Drawable> {
        return when (val model = requestModel.model) {
            is Int -> fallback(model).load(null as Any?)
            is File -> load(model)
            is Uri -> load(model)
            is String -> load(model)
            else -> load(model)
        }
    }

    private fun RequestBuilder<Drawable>.setupPlaceholder(): RequestBuilder<Drawable> {
        return when (val model = loadingModel) {
            is ResModel -> placeholder(model.id)
            else -> this
        }
    }

    private fun RequestBuilder<Drawable>.setupFailure(): RequestBuilder<Drawable> {
        return when (val model = failureModel) {
            is ResModel -> error(model.id)
            else -> this
        }
    }

    private fun RequestBuilder<Drawable>.setupSize(): RequestBuilder<Drawable> {
        return when (val size = glideSize.readySize()) {
            null -> this
            else -> override(size.width, size.height)
        }
    }

    private suspend fun flowRequest(requestModel: GlideRequestModel) {

        requestModel.requestBuilder()
            .setupSize()
            .setupScaleTransform()
            .setupPlaceholder()
            .setupFailure()
            .loadRequestModel(requestModel)
            .flow(glideSize, requestModel.listener)
            .collectLatest {
                log("startRequest") { "collectLatest $it" }
                val result = when (it) {
                    is GlideLoadResult.Error -> {
                        it.drawable?.toPainter()
                            ?: (failureModel as? PainterModel)?.painter
                            ?: (failureModel as? DrawableModel)?.drawable?.toPainter()
                            ?: placeablePainter
                    }

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

    protected fun stopRequest() {
        rememberJob?.cancel()
        rememberJob = null
        // finish all flow target
        if (!glideSize.sizeReady()) glideSize.putSize(Size.Unspecified)
    }

    protected fun startAnimation(painter: Painter) {
        if (painter is AnimatablePainter) painter.startAnimation()
    }

    protected fun stopAnimation(painter: Painter) {
        if (painter is AnimatablePainter) painter.stopAnimation()
    }


    /**
     * Setup state and start request.
     *
     * It may be call when node attach or update
     */
    open fun setup() {
        startAnimation(painter)
        startRequest(nodeModel)
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

    open fun update(nodeModel: GlideNodeModel) {
        if (nodeModel == this.nodeModel) return
        // if different model, reset all and restart request
        log("update") { "${this.nodeModel} -> $nodeModel" }
        this.nodeModel = nodeModel
        reset()
        setup()
    }

    override fun onAttach() {
        super.onAttach()
        log("attach") { "$nodeModel" }
        setup()
    }

    override fun onDetach() {
        super.onDetach()
        log("detach") { "$nodeModel" }
        reset()
    }

    override fun onReset() {
        super.onReset()
        log("reset") { "$nodeModel" }
        reset()
    }
}