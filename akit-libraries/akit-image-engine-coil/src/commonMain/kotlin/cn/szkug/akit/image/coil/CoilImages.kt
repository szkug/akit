package cn.szkug.akit.image.coil

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchPainter
import coil3.Image

internal class NinePatchCoilImage(
    private val image: Image,
    val content: ImageBitmap,
    val chunk: NinePatchChunk,
) : Image {
    private val painter by lazy { NinePatchPainter(content, chunk) }

    override val size: Long get() = image.size
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val shareable: Boolean get() = image.shareable

    override fun draw(canvas: coil3.Canvas) {
        image.draw(canvas)
    }

    fun toPainter(): Painter = painter
}

internal class GifCoilImage(
    private val firstFrame: Image?,
    private val painter: Painter,
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean = false,
) : Image {
    override fun draw(canvas: coil3.Canvas) {
        firstFrame?.draw(canvas)
    }

    fun toPainter(): Painter = painter
}

internal class LottieCoilImage(
    private val firstFrame: Image?,
    private val painter: Painter,
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean = false,
) : Image {
    override fun draw(canvas: coil3.Canvas) {
        firstFrame?.draw(canvas)
    }

    fun toPainter(): Painter = painter
}
