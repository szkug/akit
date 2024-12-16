package com.korilin.compose.akit.image.glide

import android.graphics.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt


internal fun Modifier.glideBackground(
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    extension: GlideExtension,
): Modifier = this then GlideBackgroundElement(
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    extension = extension,
)

private data class GlideBackgroundElement(
    val nodeModel: GlideNodeModel,
    val loadingModel: GlidePlaceholderModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val extension: GlideExtension
) : ModifierNodeElement<GlideBackgroundNode>() {

    override fun create(): GlideBackgroundNode {
        return GlideBackgroundNode(
            nodeModel = nodeModel,
            loadingModel = loadingModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            extension = extension,
        )
    }

    override fun update(node: GlideBackgroundNode) {
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.extension = extension

        node.update(nodeModel, loadingModel, loadingModel)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GlideBackground"
        properties["model"] = nodeModel
        properties["placeholder"] = loadingModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

private class GlideBackgroundNode(
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel?,
    contentScale: ContentScale,
    var alignment: Alignment,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    extension: GlideExtension
) : GlideRequestNode(
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    failureModel = loadingModel,
    contentScale = contentScale,
    extension = extension
), LayoutModifierNode, DrawModifierNode {

    override val glideSize: ResolvableGlideSize = extension.resolveSize?.let {
        ImmediateGlideSize(it)
    } ?: AsyncGlideSize()

    override fun onCollectResult(result: GlideLoadResult) {
        invalidateMeasurement()
        invalidateDraw()
    }

    private fun drawPadding() = if (!extension.ignoreNinePatchPadding) {
        (painter as? NinePatchPainter)?.padding ?: Rect()
    } else Rect()

    // TODO Handle adaptive width and height scenarios with different ContentScale using modifyConstraints
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val inferredGlideSize = constraints.inferredGlideSize()
        glideSize.putSize(inferredGlideSize)

        // measure with padding
        val padding: Rect = drawPadding()

        val horizontal = padding.left + padding.right
        val vertical = padding.top + padding.bottom

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))
        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)

        return layout(width, height) {
            placeable.placeRelative(padding.left, padding.top)
        }

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

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // plus draw padding
        val padding: Rect = drawPadding()
        val horizontal = padding.left + padding.right
        val vertical = padding.top + padding.bottom
        val drawSize = Size(scaledSize.width + horizontal, scaledSize.height + vertical)

        translate(dx - padding.left, dy - padding.top) {
            drawIntoCanvas {
                it.withSave {
                    with(painter) {
                        draw(size = drawSize, alpha = alpha, colorFilter = colorFilter)
                    }
                }
            }
        }

        drawContent()
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