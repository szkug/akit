package cn.szkug.akit.image

import androidx.compose.ui.Alignment
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
import kotlin.math.max
import kotlin.math.roundToInt

private const val TRACE_SECTION_NAME = "AsyncPainterModifier"

internal fun Modifier.asyncPainterNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    failureModel: ResourceModel?,
    contentDescription: String?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    imageContext: AsyncImageContext,
    engineContext: EngineContext,
    engine: AsyncRequestEngine<*>
): Modifier = clipToBounds()
    .semantics {
        if (contentDescription != null) {
            this@semantics.contentDescription = contentDescription
        }
        role = Role.Image
    } then AsyncPainterElement(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = failureModel,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    imageContext = imageContext,
    engineContext = engineContext,
    engine = engine.asAsyncLoadDataEngine()
)

private data class AsyncPainterElement(
    val requestModel: RequestModel,
    val placeholderModel: PainterModel?,
    val failureModel: ResourceModel?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val imageContext: AsyncImageContext,
    val engineContext: EngineContext,
    val engine: AsyncRequestEngine<AsyncLoadData>
) : ModifierNodeElement<AsyncPainterNode>() {

    override fun create(): AsyncPainterNode {
        return AsyncPainterNode(
            requestModel = requestModel,
            placeholderModel = placeholderModel,
            failureModel = failureModel,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            imageContext = imageContext,
            engineContext = engineContext,
            engine = engine
        )
    }

    override fun update(node: AsyncPainterNode) {
        node.alignment = alignment
        node.alpha = alpha
        node.colorFilter = colorFilter

        node.update(requestModel, placeholderModel, failureModel, contentScale, imageContext, engineContext)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "AsyncPainter"
        properties["requestModel"] = requestModel
        properties["placeholderModel"] = placeholderModel
        properties["failureModel"] = failureModel
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

internal class AsyncPainterNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    failureModel: ResourceModel?,
    contentScale: ContentScale,
    var alignment: Alignment,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    imageContext: AsyncImageContext,
    engineContext: EngineContext,
    engine: AsyncRequestEngine<AsyncLoadData>
) : AsyncRequestNode(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = failureModel,
    contentScale = contentScale,
    imageContext = imageContext,
    engineContext = engineContext,
    engine = engine
), LayoutModifierNode, DrawModifierNode {

    private var hasFixedSize: Boolean = false

    private fun Constraints.hasFixedSize() = hasFixedWidth && hasFixedHeight

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = trace("$TRACE_SECTION_NAME.measure") {
        val modified = modifyConstraints(constraints)

        val inferredSize = modified.inferredSize()

        size.putSize(inferredSize)
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

    /**
     * Helper property to determine if we should size content to the intrinsic
     * size of the Painter or not. This is only done if the Painter has an intrinsic size
     */
    private val painterIntrinsicSizeSpecified: Boolean
        get() = painter.intrinsicSize.isSpecified

    private fun modifyConstraints(constraints: Constraints): Constraints =
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

    override fun onCollectResult(painter: Painter?) {
        super.onCollectResult(painter)
        if (!hasFixedSize) {
            invalidateMeasurement()
        }
        invalidateDraw()
    }

    override fun update(
        requestModel: RequestModel,
        placeholderModel: PainterModel?,
        failureModel: ResourceModel?,
        contentScale: ContentScale,
        imageContext: AsyncImageContext,
        engineContext: EngineContext,
    ) {
        super.update(requestModel, placeholderModel, failureModel, contentScale, imageContext, engineContext)
        invalidateMeasurement()
        invalidateDraw()
    }
}
