package munchkin.svga

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * Stateless renderer used by the SVGA modifier node.
 *
 * The node owns loading and playback. This renderer only maps one decoded movie plus the current
 * runtime overrides into draw commands for the active frame.
 */
internal class SvgaRenderer(
    private val prepared: PreparedSvgaMovie,
    private val dynamicEntity: SvgaDynamicEntity,
    private val contentScale: ContentScale,
    private val alignment: Alignment,
    private val textMeasurer: TextMeasurer,
) {

    val intrinsicSize: Size = Size(
        prepared.movie.width.takeIf { it > 0f } ?: Size.Unspecified.width,
        prepared.movie.height.takeIf { it > 0f } ?: Size.Unspecified.height,
    )

    fun DrawScope.draw(frameIndex: Int): Map<String, Rect> {
        dynamicEntity.revision
        val movie = prepared.movie
        if (movie.width <= 0f || movie.height <= 0f) return emptyMap()
        val resolvedFrameIndex = frameIndex.coerceIn(0, movie.frames.coerceAtLeast(1) - 1)
        val movieMatrix = computeMovieMatrix(movie.width, movie.height, size)
        val hitRegions = linkedMapOf<String, Rect>()
        val active = movie.sprites.mapNotNull { sprite ->
            sprite.frames.getOrNull(resolvedFrameIndex)?.let { frame -> SvgaSpriteSnapshot(sprite, frame) }
        }
        var index = 0
        while (index < active.size) {
            val snapshot = active[index]
            val matteKey = snapshot.sprite.matteKey
            if (matteKey.isNullOrBlank()) {
                drawSprite(snapshot, movieMatrix, resolvedFrameIndex, hitRegions, BlendMode.SrcOver)
                index += 1
                continue
            }
            val group = mutableListOf<SvgaSpriteSnapshot>()
            while (index < active.size && active[index].sprite.matteKey == matteKey) {
                group += active[index]
                index += 1
            }
            val matte = active.firstOrNull {
                it.sprite.imageKey == matteKey || normalizeBitmapAssetKey(it.sprite.imageKey) == normalizeBitmapAssetKey(matteKey)
            }
            if (matte == null) {
                group.forEach { drawSprite(it, movieMatrix, resolvedFrameIndex, hitRegions, BlendMode.SrcOver) }
                continue
            }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(groupBounds(group + matte, movieMatrix), Paint())
            }
            group.forEach { drawSprite(it, movieMatrix, resolvedFrameIndex, hitRegions, BlendMode.SrcOver) }
            drawSprite(matte, movieMatrix, resolvedFrameIndex, hitRegions, BlendMode.DstIn)
            drawIntoCanvas { canvas -> canvas.restore() }
        }
        return hitRegions
    }

    private fun DrawScope.drawSprite(
        snapshot: SvgaSpriteSnapshot,
        movieMatrix: Matrix,
        frameIndex: Int,
        hitRegions: MutableMap<String, Rect>,
        blendMode: BlendMode,
    ) {
        val key = snapshot.sprite.imageKey
        if (dynamicEntity.hidden[key] == true) return
        val frame = snapshot.frame
        dynamicEntity.drawers[key]?.let { drawer ->
            var consumed = false
            withTransform({
                transform(movieMatrix)
                transform(frame.transform.toComposeMatrix())
            }) {
                consumed = drawer(SvgaDynamicDrawContext(key, frameIndex, frame.layout))
            }
            if (consumed) return
        }
        if (dynamicEntity.hidden[key] == true) return
        if (dynamicEntity.hasClickHandler(key)) {
            hitRegions[key] = movieMatrix.map(frame.mappedBounds())
        }
        withTransform({
            transform(movieMatrix)
            transform(frame.transform.toComposeMatrix())
        }) {
            val drawBlock: DrawScope.() -> Unit = {
                when {
                    frame.shapes.isNotEmpty() -> drawShapes(frame.shapes, frame.alpha, blendMode)
                    dynamicEntity.texts[key] != null -> drawDynamicText(frame.layout, dynamicEntity.texts.getValue(key), blendMode)
                    dynamicEntity.visuals[key] != null -> drawDynamicVisual(frame.layout, dynamicEntity.visuals.getValue(key), frame.alpha, blendMode)
                    else -> drawBitmapSprite(key, frame.layout, frame.alpha, blendMode)
                }
            }
            val clip = frame.clipPathOrNull()
            if (clip != null) {
                clipPath(clip) { drawBlock() }
            } else {
                drawBlock()
            }
        }
    }

    private fun DrawScope.drawBitmapSprite(
        key: String,
        layout: SvgaLayout,
        alpha: Float,
        blendMode: BlendMode,
    ) {
        val bitmap = prepared.bitmaps[normalizeBitmapAssetKey(key)] ?: return
        drawImage(
            image = bitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset(layout.x.roundToInt(), layout.y.roundToInt()),
            dstSize = IntSize(layout.width.roundToInt().coerceAtLeast(1), layout.height.roundToInt().coerceAtLeast(1)),
            alpha = alpha,
            blendMode = blendMode,
        )
    }

    private fun DrawScope.drawDynamicVisual(
        layout: SvgaLayout,
        visual: SvgaDynamicVisual,
        alpha: Float,
        blendMode: BlendMode,
    ) {
        when (visual) {
            is SvgaDynamicVisual.Bitmap -> drawImage(
                image = visual.image,
                dstOffset = androidx.compose.ui.unit.IntOffset(layout.x.roundToInt(), layout.y.roundToInt()),
                dstSize = IntSize(layout.width.roundToInt().coerceAtLeast(1), layout.height.roundToInt().coerceAtLeast(1)),
                alpha = alpha,
                blendMode = blendMode,
            )

            is SvgaDynamicVisual.PainterValue -> withTransform({
                translate(layout.x, layout.y)
            }) {
                with(visual.painter) {
                    draw(
                        size = Size(layout.width, layout.height),
                        alpha = alpha,
                        colorFilter = null,
                    )
                }
            }
        }
    }

    private fun DrawScope.drawDynamicText(
        layout: SvgaLayout,
        text: SvgaDynamicText,
        blendMode: BlendMode,
    ) {
        drawText(
            textMeasurer = textMeasurer,
            text = AnnotatedString(text.text),
            topLeft = Offset(layout.x, layout.y),
            style = text.style,
            overflow = text.overflow,
            softWrap = text.softWrap,
            maxLines = text.maxLines,
            size = Size(layout.width, layout.height),
            blendMode = blendMode,
        )
    }

    private fun DrawScope.drawShapes(
        shapes: List<SvgaShape>,
        alpha: Float,
        blendMode: BlendMode,
    ) {
        shapes.forEach { shape ->
            if (shape.type == SvgaShapeType.Keep) return@forEach
            val path = shape.buildPath() ?: return@forEach
            withTransform({
                shape.transform?.let { transform(it.toComposeMatrix()) }
            }) {
                shape.style?.fill?.let { fill ->
                    drawPath(path = path, color = fill, alpha = alpha, style = Fill, blendMode = blendMode)
                }
                shape.style?.stroke?.let { stroke ->
                    drawPath(
                        path = path,
                        color = stroke,
                        alpha = alpha,
                        style = Stroke(
                            width = shape.style.strokeWidth,
                            miter = shape.style.miterLimit,
                            cap = when (shape.style.lineCap) {
                                SvgaLineCap.Round -> StrokeCap.Round
                                SvgaLineCap.Square -> StrokeCap.Square
                                SvgaLineCap.Butt -> StrokeCap.Butt
                            },
                            join = when (shape.style.lineJoin) {
                                SvgaLineJoin.Bevel -> StrokeJoin.Bevel
                                SvgaLineJoin.Round -> StrokeJoin.Round
                                SvgaLineJoin.Miter -> StrokeJoin.Miter
                            },
                            pathEffect = shape.style.lineDash.takeIf { it.size >= 2 }?.let { PathEffect.dashPathEffect(it, 0f) },
                        ),
                        blendMode = blendMode,
                    )
                }
            }
        }
    }

    private fun SvgaShape.buildPath(): Path? {
        return when (val data = data) {
            is SvgaShapeData.PathData -> PathParser().parsePathString(data.value).toPath()
            is SvgaShapeData.RectData -> Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(data.x, data.y, data.x + data.width, data.y + data.height),
                        topLeft = androidx.compose.ui.geometry.CornerRadius(data.cornerRadius, data.cornerRadius),
                        topRight = androidx.compose.ui.geometry.CornerRadius(data.cornerRadius, data.cornerRadius),
                        bottomRight = androidx.compose.ui.geometry.CornerRadius(data.cornerRadius, data.cornerRadius),
                        bottomLeft = androidx.compose.ui.geometry.CornerRadius(data.cornerRadius, data.cornerRadius),
                    )
                )
            }

            is SvgaShapeData.EllipseData -> Path().apply {
                addOval(Rect(data.x - data.radiusX, data.y - data.radiusY, data.x + data.radiusX, data.y + data.radiusY))
            }

            null -> null
        }
    }

    private fun computeMovieMatrix(movieWidth: Float, movieHeight: Float, canvasSize: Size): Matrix {
        val scaleFactor = contentScale.computeScaleFactor(Size(movieWidth, movieHeight), canvasSize)
        val scaled = Size(movieWidth * scaleFactor.scaleX, movieHeight * scaleFactor.scaleY)
        val offset = alignment.align(
            size = IntSize(scaled.width.roundToInt(), scaled.height.roundToInt()),
            space = IntSize(canvasSize.width.roundToInt(), canvasSize.height.roundToInt()),
            layoutDirection = LayoutDirection.Ltr,
        )
        return Matrix().apply {
            this[0, 0] = scaleFactor.scaleX
            this[1, 1] = scaleFactor.scaleY
            this[3, 0] = offset.x.toFloat()
            this[3, 1] = offset.y.toFloat()
        }
    }

    private fun groupBounds(
        sprites: List<SvgaSpriteSnapshot>,
        movieMatrix: Matrix,
    ): Rect {
        return sprites.map { movieMatrix.map(it.frame.mappedBounds()) }.reduceOrNull { acc, rect ->
            Rect(
                left = minOf(acc.left, rect.left),
                top = minOf(acc.top, rect.top),
                right = maxOf(acc.right, rect.right),
                bottom = maxOf(acc.bottom, rect.bottom),
            )
        } ?: Rect.Zero
    }
}

private data class SvgaSpriteSnapshot(
    val sprite: SvgaSprite,
    val frame: SvgaFrame,
)
