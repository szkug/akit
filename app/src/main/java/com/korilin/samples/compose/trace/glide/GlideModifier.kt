package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.trace
import com.bumptech.glide.RequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

sealed interface GlideNodeModel
sealed interface GlidePlaceholderModel

internal class GlideRequestModel(
    val model: Any?,
    val requestBuilder: () -> RequestBuilder<Drawable>,
    val listener: PainterRequestListener?,
) : GlideNodeModel {

    override fun equals(other: Any?): Boolean {
        if (other !is GlideRequestModel) return false
        return other.model == model
    }

    override fun toString(): String {
        return "GlideRequestModel($model)"
    }

    override fun hashCode(): Int {
        return model?.hashCode() ?: 0
    }
}

@JvmInline
value class PainterModel(val painter: Painter) : GlideNodeModel, GlidePlaceholderModel

@JvmInline
value class ResModel(@DrawableRes val id: Int) : GlidePlaceholderModel

internal fun Modifier.glidePainterNode(
    tag: String? = null,
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel? = null,
    failureModel: GlidePlaceholderModel? = null,
    contentDescription: String? = null,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter? = null,
): Modifier = clipToBounds()
    .semantics {
        if (contentDescription != null) {
            this@semantics.contentDescription = contentDescription
        }
        role = Role.Image
    } then GlidePainterElement(
    tag = tag,
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    failureModel = failureModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
)

internal data class GlidePainterElement(
    val tag: String? = null,
    val nodeModel: GlideNodeModel,
    val loadingModel: GlidePlaceholderModel?,
    val failureModel: GlidePlaceholderModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?
) : ModifierNodeElement<GlidePainterNode>() {

    override fun create(): GlidePainterNode {
        return GlidePainterNode(
            tag = tag,
            nodeModel = nodeModel,
            loadingModel = loadingModel,
            failureModel = failureModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    override fun update(node: GlidePainterNode) {
        node.tag = tag
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.loadingModel = loadingModel
        node.failureModel = failureModel

        node.update(nodeModel)

        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GlidePainter"
        properties["tag"] = tag
        properties["model"] = nodeModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

internal class GlidePainterNode(
    var tag: String?,
    var nodeModel: GlideNodeModel,
    var loadingModel: GlidePlaceholderModel?,
    var failureModel: GlidePlaceholderModel?,
    var alignment: Alignment,
    var contentScale: ContentScale,
    var alpha: Float,
    var colorFilter: ColorFilter?,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode {

    private val loadingPainter get() = (loadingModel as? PainterModel)?.painter
    private val errorPainter get() = (failureModel as? PainterModel)?.painter

    private val placeablePainter
        get() = when (val model = nodeModel) {
            is GlideRequestModel -> null
            is PainterModel -> model.painter
        } ?: loadingPainter ?: EmptyPainter

    private var painter = placeablePainter
        set(value) = trace("GlidePainterNode.painter.setter") {
            if (field == value) return
            stopAnimation(field)
            field = value
            log { "update painter $field" }
        }

    private fun startAnimation() {
        val cur = painter
        if (cur is DrawablePainter) cur.startAnimation()
        log("startAnimation") { "$painter" }
    }

    private fun stopAnimation(painter: Painter) {
        if (painter is DrawablePainter) painter.stopAnimation()
        log("stopAnimation") { "$painter" }
    }

    private val glideSize = AsyncGlideSize()

    /**
     * Helper property to determine if we should size content to the intrinsic
     * size of the Painter or not. This is only done if the Painter has an intrinsic size
     */
    private val painterIntrinsicSizeSpecified: Boolean
        get() = painter.intrinsicSize.isSpecified

    override val shouldAutoInvalidate: Boolean
        get() = false

    private inline fun log(subtag: String? = null, message: () -> String) {
        // skip redundant string concatenation
        if (GlidePainterLogger.LOGGER_ENABLE)
            GlidePainterLogger.log("GlidePainterNode[$tag]") {
                if (subtag == null) message()
                else "[$subtag] ${message()}"
            }
    }

    private var hasFixedSize: Boolean = false
    private fun Constraints.hasFixedSize() = hasFixedWidth && hasFixedHeight

    private fun Constraints.inferredGlideSize(): Size {
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


    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val modified = modifyConstraints(constraints)
        hasFixedSize = modified.hasFixedSize()
        val inferredGlideSize = modified.inferredGlideSize()
        log("measure") { "$constraints -> $modified, $inferredGlideSize" }
        val placeable = measurable.measure(modified)
        coroutineScope.launch { glideSize.emit(inferredGlideSize) }
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val layoutWidth = measurable.minIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth).also {
            log("minIntrinsicWidth") { "$layoutWidth -> $it" }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val layoutWidth = measurable.maxIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth).also {
            log("maxIntrinsicWidth") { "$layoutWidth -> $it" }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val layoutHeight = measurable.minIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight).also {
            log("minIntrinsicHeight") { "$layoutHeight -> $it" }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val layoutHeight = measurable.maxIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight).also {
            log("maxIntrinsicHeight") { "$layoutHeight -> $it" }
        }
    }


    private fun modifyIntrinsicWidth(height: Int, layoutWidth: Int): Int {
        return if (painterIntrinsicSizeSpecified) {
            val constraints = modifyConstraints(Constraints(maxHeight = height))
            max(constraints.minWidth, layoutWidth)
        } else {
            layoutWidth
        }
    }

    private fun modifyIntrinsicHeight(width: Int, layoutHeight: Int): Int {
        return if (painterIntrinsicSizeSpecified) {
            val constraints = modifyConstraints(Constraints(maxWidth = width))
            max(constraints.minHeight, layoutHeight)
        } else {
            layoutHeight
        }
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

    private fun modifyConstraints(constraints: Constraints): Constraints =
        trace("GlidePainterNode.modifyConstraints") {

            // If we have fixed constraints, use the original constraints
            if (constraints.hasFixedWidth && constraints.hasFixedHeight) return constraints

            val hasBoundedDimens = constraints.hasBoundedWidth && constraints.hasBoundedHeight

            // If we are not attempting to size the composable based on the size of the Painter,
            // do not attempt to modify them.
            if (!painterIntrinsicSizeSpecified && hasBoundedDimens) {
                log("modifyConstraints") { "use bounded dimens" }
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

    override fun ContentDrawScope.draw() {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
            intrinsicSize.height
        } else {
            size.height
        }

        val srcSize = Size(srcWidth, srcHeight)

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize = if (size.width != 0f && size.height != 0f) {
            srcSize * contentScale.computeScaleFactor(srcSize, size)
        } else {
            size
        }

        log("draw") { "$size -> $scaledSize" }

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
        translate(dx, dy) {
            with(painter) {
                draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
            }
        }

        // Maintain the same pattern as Modifier.drawBehind to allow chaining of DrawModifiers
        drawContent()
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
    private fun startRequest(requestModel: GlideNodeModel) =
        sideEffect {

            log("startRequest") { "cur:$nodeModel req:$nodeModel ${rememberJob?.isActive}" }

            // The request changed while our sideEffect was queued, which should also have triggered
            // another sideEffect. Wait for that one instead.
            if (this.nodeModel != requestModel) return@sideEffect

            if (rememberJob != null || requestModel !is GlideRequestModel) return@sideEffect

            trace("GlidePainterNode.launch") {
                rememberJob = coroutineScope.launch(Dispatchers.Default) {
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

    private suspend fun flowRequest(requestModel: GlideRequestModel) {
        requestModel.requestBuilder()
            .setupScaleTransform()
            .setupPlaceholder()
            .setupFailure()
            .loadRequestModel(requestModel)
            .flow(glideSize, requestModel.listener)
            .collectLatest {
                log("startRequest") { "$it" }

                painter = when (it) {
                    is GlideLoadResult.Error -> it.painter ?: errorPainter ?: placeablePainter
                    is GlideLoadResult.Success -> it.painter
                    is GlideLoadResult.Cleared -> placeablePainter
                }

                startAnimation()

                if (!hasFixedSize) {
                    invalidateMeasurement()
                }
                invalidateDraw()
            }
    }

    private fun stopRequest() {
        rememberJob?.cancel()
        rememberJob = null
        // finish all flow target
        if (!glideSize.hasEmit) glideSize.tryEmit(Size.Unspecified)
    }

    /**
     * Setup state and start request.
     *
     * It may be call when node attach or update
     */
    private fun setup() {
        startAnimation()
        startRequest(nodeModel)
    }

    /**
     * Reset all state and stop request.
     *
     * It may be call node detach/reset or model [update]
     */
    private fun reset() {
        stopRequest()
        painter = placeablePainter
    }

    fun update(nodeModel: GlideNodeModel) {
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

    private fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    private fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    override fun toString(): String =
        "GlidePainterNode(" +
                "nodeModel=$nodeModel, " +
                "painter=$painter, " +
                "alignment=$alignment, " +
                "alpha=$alpha, " +
                "colorFilter=$colorFilter)"
}
