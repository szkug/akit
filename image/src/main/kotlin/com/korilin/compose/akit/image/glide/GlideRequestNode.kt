package com.korilin.compose.akit.image.glide

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.trace
import com.bumptech.glide.RequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val TRACE_SECTION_NAME = "GlideRequestNode"

internal abstract class GlideRequestNode(
    private var nodeModel: GlideNodeModel,
    private var loadingModel: GlidePlaceholderModel?,
    private var failureModel: GlidePlaceholderModel?,
    var contentScale: ContentScale,
    var extension: GlideExtension,
) : Modifier.Node() {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()
    protected fun Constraints.hasFixedSize() = hasFixedWidth && hasFixedHeight

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
        if (extension.enableLog)
            GlideDefaults.logger.info(TRACE_SECTION_NAME) {
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

            // TODO fitCenter might be better in adaptive scenarios?
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

    private fun Drawable.transcodeDrawable(): Painter {
        val transcoder = extension.transcoder
        val drawable = transcoder?.transcode(this) ?: this
        return drawable.toPainter()
    }

    private suspend fun flowRequest(requestModel: GlideRequestModel) {

        requestModel.requestBuilder()
            .setupSize()
            .setupScaleTransform()
            .setupPlaceholder()
            .setupFailure()
            .loadRequestModel(requestModel)
            .flow(glideSize)
            .collectLatest {
                log("startRequest") { "collectLatest $it" }
                val result = when (it) {
                    is GlideLoadResult.Error -> {
                        it.drawable?.transcodeDrawable()
                            ?: (failureModel as? PainterModel)?.painter
                            ?: (failureModel as? DrawableModel)?.drawable?.transcodeDrawable()
                            ?: placeablePainter
                    }

                    is GlideLoadResult.Success -> it.drawable.transcodeDrawable()
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

    open fun update(
        nodeModel: GlideNodeModel,
        loadingModel: GlidePlaceholderModel?,
        failureModel: GlidePlaceholderModel?
    ) {
        var hasModify = false
        if (nodeModel != this.nodeModel) {
            this.nodeModel = nodeModel
            hasModify = true
        }

        if (loadingModel != this.loadingModel) {
            this.loadingModel = loadingModel
            hasModify = true
        }
        if (failureModel != this.failureModel) {
            this.failureModel = failureModel
            hasModify = true
        }
        if (!hasModify) return
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


    /**
     * Helper property to determine if we should size content to the intrinsic
     * size of the Painter or not. This is only done if the Painter has an intrinsic size
     */
    protected val painterIntrinsicSizeSpecified: Boolean
        get() = painter.intrinsicSize.isSpecified


    /**
     * By comparing [androidx.compose.ui.draw].NodePainter, coil3-compose(rc02), glide-compose(1.0.0-beta01),
     * here make some adaptive adjustment of the width and height.
     */
    protected fun modifyConstraints(constraints: Constraints): Constraints =
        trace("$TRACE_SECTION_NAME.modifyConstraints") {

            // If we have fixed constraints, use the original constraints
            if (constraints.hasFixedWidth && constraints.hasFixedHeight) return constraints

            val hasBoundedDimens = constraints.hasBoundedWidth && constraints.hasBoundedHeight

            // If we are not attempting to size the composable based on the size of the Painter,
            // do not attempt to modify them.
            if (!painterIntrinsicSizeSpecified && hasBoundedDimens) {
                return constraints
            }

            // Otherwise rely on Alignment and ContentScale to determine
            // how to position the drawing contents of the Painter within the provided bounds

            val intrinsicSize = painter.intrinsicSize
            val intrinsicWidth =
                if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                    intrinsicSize.width.roundToInt()
                } else {
                    constraints.minWidth
                }

            val intrinsicHeight =
                if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                    intrinsicSize.height.roundToInt()
                } else {
                    constraints.minHeight
                }

            // Scale the width and height appropriately based on the given constraints
            // and ContentScale
            val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
            val constrainedHeight = constraints.constrainHeight(intrinsicHeight)
            val scaledSize = calculateScaledSize(
                Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
            )

            // For both width and height constraints, consume the minimum of the scaled width
            // and the maximum constraint as some scale types can scale larger than the maximum
            // available size (ex ContentScale.Crop)
            // In this case the larger of the 2 dimensions is used and the aspect ratio is
            // maintained. Even if the size of the composable is smaller, the painter will
            // draw its content clipped
            val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
            val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
            return constraints.copy(minWidth = minWidth, minHeight = minHeight)
        }



    private fun calculateScaledSize(dstSize: Size): Size {
        return if (painterIntrinsicSizeSpecified) {
            val srcWidth = if (!painter.intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                dstSize.width
            } else {
                painter.intrinsicSize.width
            }

            val srcHeight = if (!painter.intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                dstSize.height
            } else {
                painter.intrinsicSize.height
            }

            val srcSize = Size(srcWidth, srcHeight)
            if (dstSize.width != 0f && dstSize.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
            } else {
                Size.Zero
            }

        } else {
            dstSize
        }
    }
}