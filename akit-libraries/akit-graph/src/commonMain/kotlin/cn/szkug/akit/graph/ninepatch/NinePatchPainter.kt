package cn.szkug.akit.graph.ninepatch

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import cn.szkug.akit.graph.HasPaddingPainter
import cn.szkug.akit.graph.ImagePadding
import kotlin.math.roundToInt

class NinePatchPainter(
    private val image: ImageBitmap,
    private val chunk: NinePatchChunk
) : HasPaddingPainter() {

    override val padding: ImagePadding = ImagePadding(
        left = chunk.padding.left,
        top = chunk.padding.top,
        right = chunk.padding.right,
        bottom = chunk.padding.bottom,
    )

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val contentWidth = image.width
        val contentHeight = image.height
        if (contentWidth <= 0 || contentHeight <= 0) return

        val xSegments = buildSegments(chunk.xDivs, contentWidth)
        val ySegments = buildSegments(chunk.yDivs, contentHeight)
        if (xSegments.isEmpty() || ySegments.isEmpty()) return

        val destWidths = computeDestLengths(xSegments, size.width.roundToInt())
        val destHeights = computeDestLengths(ySegments, size.height.roundToInt())

        var destY = 0
        for (yIndex in ySegments.indices) {
            val ySeg = ySegments[yIndex]
            val dstH = destHeights[yIndex]
            if (dstH <= 0) {
                continue
            }
            var destX = 0
            for (xIndex in xSegments.indices) {
                val xSeg = xSegments[xIndex]
                val dstW = destWidths[xIndex]
                if (dstW <= 0) {
                    continue
                }

                val srcX = xSeg.start
                val srcY = ySeg.start
                val srcW = xSeg.length
                val srcH = ySeg.length

                if (srcW > 0 && srcH > 0) {
                    drawImageRect(
                        image = image,
                        src = IntRect(srcX, srcY, srcX + srcW, srcY + srcH),
                        dst = IntRect(destX, destY, destX + dstW, destY + dstH)
                    )
                }
                destX += dstW
            }
            destY += dstH
        }
    }

    private data class Segment(
        val start: Int,
        val endExclusive: Int,
        val stretch: Boolean
    ) {
        val length: Int get() = (endExclusive - start).coerceAtLeast(0)
    }

    private fun buildSegments(divs: List<NinePatchDiv>, max: Int): List<Segment> {
        if (max <= 0) return emptyList()
        val out = ArrayList<Segment>()
        var cursor = 0
        for (div in divs) {
            val start = div.start.coerceIn(0, max)
            val end = div.stop.coerceIn(0, max)
            if (start > cursor) {
                out.add(Segment(cursor, start, stretch = false))
            }
            if (end > start) {
                out.add(Segment(start, end, stretch = true))
                cursor = end
            }
        }
        if (cursor < max) {
            out.add(Segment(cursor, max, stretch = false))
        }
        return out
    }

    private fun computeDestLengths(segments: List<Segment>, destLength: Int): IntArray {
        if (segments.isEmpty()) return IntArray(0)
        if (destLength <= 0) return IntArray(segments.size)

        val fixedTotal = segments.filterNot { it.stretch }.sumOf { it.length }
        val stretchTotal = segments.filter { it.stretch }.sumOf { it.length }
        val lengths = IntArray(segments.size)

        if (destLength < fixedTotal || stretchTotal == 0) {
            val scale = if (fixedTotal > 0) destLength.toFloat() / fixedTotal else 0f
            var used = 0
            for (i in segments.indices) {
                val length = (segments[i].length * scale).roundToInt()
                lengths[i] = length
                used += length
            }
            lengths[segments.lastIndex] += destLength - used
            return lengths
        }

        val remaining = destLength - fixedTotal
        val scale = if (stretchTotal > 0) remaining.toFloat() / stretchTotal else 0f
        var used = 0
        for (i in segments.indices) {
            val seg = segments[i]
            val length = if (seg.stretch) (seg.length * scale).roundToInt() else seg.length
            lengths[i] = length
            used += length
        }
        lengths[segments.lastIndex] += destLength - used
        return lengths
    }
}

private fun DrawScope.drawImageRect(
    image: ImageBitmap,
    src: IntRect,
    dst: IntRect,
) {
    val srcOffset = IntOffset(src.left, src.top)
    val srcSize = IntSize(src.width, src.height)
    val dstOffset = IntOffset(dst.left, dst.top)
    val dstSize = IntSize(dst.width, dst.height)
    val paint = Paint()
    drawIntoCanvas { canvas ->
        canvas.drawImageRect(image, srcOffset, srcSize, dstOffset, dstSize, paint)
    }
}

class ImageBitmapNinePatchSource(
    private val image: ImageBitmap
) : NinePatchPixelSource {

    private val pixelMap by lazy { image.toPixelMap() }

    override val width: Int get() = image.width
    override val height: Int get() = image.height

    override fun getPixel(x: Int, y: Int): Int {
        return pixelMap[x, y].toArgb()
    }
}
