package com.korilin.compose.akit.image.glide

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlin.math.max
import kotlin.math.roundToInt


internal fun Modifier.glidePainterNode(
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel?,
    failureModel: GlidePlaceholderModel?,
    contentDescription: String?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    extension: GlideExtension,
): Modifier = clipToBounds()
    .semantics {
        if (contentDescription != null) {
            this@semantics.contentDescription = contentDescription
        }
        role = Role.Image
    } then GlidePainterElement(
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    failureModel = failureModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    extension = extension,
)

private data class GlidePainterElement(
    val nodeModel: GlideNodeModel,
    val loadingModel: GlidePlaceholderModel?,
    val failureModel: GlidePlaceholderModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val extension: GlideExtension
) : ModifierNodeElement<GlidePainterNode>() {

    override fun create(): GlidePainterNode {
        return GlidePainterNode(
            nodeModel = nodeModel,
            loadingModel = loadingModel,
            failureModel = failureModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            extension = extension,
        )
    }

    override fun update(node: GlidePainterNode) {
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.extension = extension

        node.update(nodeModel, loadingModel, failureModel)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GlidePainter"
        properties["model"] = nodeModel
        properties["loadingModel"] = loadingModel
        properties["failureModel"] = failureModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

private const val TRACE_SECTION_NAME = "GlidePainterNode"

internal class GlidePainterNode(
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel?,
    failureModel: GlidePlaceholderModel?,
    contentScale: ContentScale,
    var alignment: Alignment,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    extension: GlideExtension
) : GlideRequestNode(
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    failureModel = failureModel,
    contentScale = contentScale,
    extension = extension
), LayoutModifierNode, DrawModifierNode {

    override val glideSize = AsyncGlideSize()


    override val shouldAutoInvalidate: Boolean
        get() = false

    private var hasFixedSize: Boolean = false
    private fun Constraints.hasFixedSize() = hasFixedWidth && hasFixedHeight


    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = trace("$TRACE_SECTION_NAME.measure") {
        val modified = modifyConstraints(constraints)

        val inferredGlideSize = modified.inferredGlideSize()

        glideSize.putSize(inferredGlideSize)

        hasFixedSize = modified.hasFixedSize()

        val placeable = measurable.measure(modified)

        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val layoutWidth = measurable.minIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val layoutWidth = measurable.maxIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val layoutHeight = measurable.minIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val layoutHeight = measurable.maxIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight)
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

    override fun ContentDrawScope.draw() = trace("$TRACE_SECTION_NAME.draw") {
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

    override fun onCollectResult(result: GlideLoadResult) {
        if (!hasFixedSize) {
            invalidateMeasurement()
        }
        invalidateDraw()
    }

    override fun update(
        nodeModel: GlideNodeModel,
        loadingModel: GlidePlaceholderModel?,
        failureModel: GlidePlaceholderModel?
    ) {
        super.update(nodeModel, loadingModel, failureModel)
        invalidateMeasurement()
        invalidateDraw()
    }
}
