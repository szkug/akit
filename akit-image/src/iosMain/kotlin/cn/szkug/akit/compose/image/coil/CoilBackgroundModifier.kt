package cn.szkug.akit.compose.image.coil

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
import cn.szkug.akit.compose.image.HasPaddingPainter
import cn.szkug.akit.compose.image.ImagePadding
import cn.szkug.akit.compose.image.RequestModel
import cn.szkug.akit.publics.AsyncImageContext
import cn.szkug.akit.publics.PainterModel
import cn.szkug.akit.publics.ResourceModel
import kotlin.math.roundToInt

internal fun Modifier.coilBackground(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    extension: AsyncImageContext,
): Modifier = this then CoilBackgroundElement(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    context = extension,
)

private data class CoilBackgroundElement(
    val requestModel: RequestModel,
    val placeholderModel: PainterModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val context: AsyncImageContext
) : ModifierNodeElement<CoilBackgroundNode>() {

    override fun create(): CoilBackgroundNode {
        return CoilBackgroundNode(
            requestModel = requestModel,
            placeholderModel = placeholderModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            context = context,
        )
    }

    override fun update(node: CoilBackgroundNode) {
        node.alignment = alignment
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.update(requestModel, placeholderModel, null, contentScale, context)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "CoilBackground"
        properties["requestModel"] = requestModel
        properties["placeholderModel"] = placeholderModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

private const val TRACE_SECTION_NAME = "CoilBackgroundNode"

private class CoilBackgroundNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    contentScale: ContentScale,
    var alignment: Alignment,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    context: AsyncImageContext
) : CoilRequestNode(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = null,
    contentScale = contentScale,
    context = context
), LayoutModifierNode, DrawModifierNode {

    override fun onCollectResult(result: CoilLoadResult<IosCachedImage>) {
        invalidateMeasurement()
        invalidateDraw()
    }

    private fun drawPadding(): ImagePadding = if (!context.ignoreImagePadding) {
        (painter as? HasPaddingPainter)?.padding ?: ImagePadding()
    } else ImagePadding()

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = trace("$TRACE_SECTION_NAME.measure") {

        val inferredSize = constraints.inferredCoilSize()
        coilSize.putSize(inferredSize)

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

        val padding = drawPadding()
        val drawSize = Size(
            scaledSize.width + padding.horizontal,
            scaledSize.height + padding.vertical,
        )

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
