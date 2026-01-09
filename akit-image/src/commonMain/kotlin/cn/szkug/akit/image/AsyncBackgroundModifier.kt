package cn.szkug.akit.image

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
import androidx.compose.ui.util.trace
import cn.szkug.akit.graph.HasPaddingPainter
import cn.szkug.akit.graph.ImagePadding
import kotlin.math.roundToInt


internal fun <Data : AsyncLoadData> Modifier.asyncBackgroundNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    context: AsyncImageContext,
    engine: AsyncRequestEngine<Data>,
): Modifier = this then AsyncBackgroundElement(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    context = context,
    engine = engine,
)

private data class AsyncBackgroundElement<Data : AsyncLoadData>(
    val requestModel: RequestModel,
    val placeholderModel: PainterModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val context: AsyncImageContext,
    val engine: AsyncRequestEngine<Data>,
) : ModifierNodeElement<AsyncBackgroundNode<Data>>() {

    override fun create(): AsyncBackgroundNode<Data> {
        return AsyncBackgroundNode(
            requestModel = requestModel,
            placeholderModel = placeholderModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            context = context,
            engine = engine
        )
    }

    override fun update(node: AsyncBackgroundNode<Data>) {
        node.alignment = alignment
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.update(requestModel, placeholderModel, null, contentScale, context)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "GlideBackground"
        properties["requestModel"] = requestModel
        properties["placeholderModel"] = placeholderModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

private const val TRACE_SECTION_NAME = "GlideBackgroundNode"

private class AsyncBackgroundNode<Data : AsyncLoadData>(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    contentScale: ContentScale,
    var alignment: Alignment,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    context: AsyncImageContext,
    engine: AsyncRequestEngine<Data>
) : AsyncRequestNode<Data>(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = null,
    contentScale = contentScale,
    context = context,
    engine = engine
), LayoutModifierNode, DrawModifierNode {

    override fun onCollectResult(result: AsyncLoadResult<Data>) {
        invalidateMeasurement()
        invalidateDraw()
    }

    private fun drawPadding(): ImagePadding = if (!context.ignoreImagePadding) {
        (painter as? HasPaddingPainter)?.padding ?: ImagePadding()
    } else ImagePadding()


    /**
     * For BackgroundNode, Node size should be controlled by the actual content,
     * and the node size should only be interfered with the image internal padding.
     *
     * If want to ignore the padding of image, set [AsyncImageContext.ignoreImagePadding] to false.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = trace("$TRACE_SECTION_NAME.measure") {

        val inferredGlideSize = constraints.inferredSize()
        size.putSize(inferredGlideSize)

        // measure with padding.
        // This could be zero.
        val padding = drawPadding()

        val horizontal = padding.horizontal
        val vertical = padding.vertical

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

        // TODO add more test cast to verify here
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

        // plus image padding into draw size
        val padding = drawPadding()
        val horizontal = padding.horizontal
        val vertical = padding.vertical
        val drawSize = Size(scaledSize.width + horizontal, scaledSize.height + vertical)

        // draw on the padding area
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
        requestModel: RequestModel,
        placeholderModel: PainterModel?,
        failureModel: ResourceModel?,
        contentScale: ContentScale,
        context: AsyncImageContext,
    ) {
        super.update(requestModel, placeholderModel, failureModel, contentScale, context)
        if (!drawPadding().isEmpty) invalidateMeasurement()
        invalidateDraw()
    }
}
