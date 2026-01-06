package cn.szkug.akit.compose.image

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

data class ImagePadding(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val horizontal: Int get() = left + right
    val vertical: Int get() = top + bottom
    val isEmpty: Boolean get() = left == 0 && top == 0 && right == 0 && bottom == 0
}

abstract class HasPaddingPainter : Painter() {
    abstract val padding: ImagePadding
}

internal object EmptyPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified
    override fun DrawScope.onDraw() {}
}
