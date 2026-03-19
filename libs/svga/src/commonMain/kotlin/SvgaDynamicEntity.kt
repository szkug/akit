package munchkin.svga

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable

sealed interface SvgaDynamicVisual {
    data class Bitmap(
        val image: ImageBitmap,
    ) : SvgaDynamicVisual

    data class PainterValue(
        val painter: Painter,
    ) : SvgaDynamicVisual
}

data class SvgaDynamicText(
    val text: String,
    val style: TextStyle = TextStyle.Default,
    val overflow: TextOverflow = TextOverflow.Clip,
    val softWrap: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
)

data class SvgaDynamicDrawContext(
    val key: String,
    val frameIndex: Int,
    val layout: SvgaLayout,
)

typealias SvgaDynamicDrawer = androidx.compose.ui.graphics.drawscope.DrawScope.(SvgaDynamicDrawContext) -> Boolean

@Stable
class SvgaDynamicEntity {
    internal val hidden = mutableStateMapOf<String, Boolean>()
    internal val visuals = mutableStateMapOf<String, SvgaDynamicVisual>()
    internal val texts = mutableStateMapOf<String, SvgaDynamicText>()
    internal val drawers = mutableStateMapOf<String, SvgaDynamicDrawer>()
    private val clickHandlers = mutableMapOf<String, (String) -> Unit>()
    private var revision by mutableIntStateOf(0)

    fun setHidden(value: Boolean, forKey: String) {
        hidden[forKey] = value
        revision += 1
    }

    fun setDynamicImage(bitmap: ImageBitmap, forKey: String) {
        visuals[forKey] = SvgaDynamicVisual.Bitmap(bitmap)
        revision += 1
    }

    fun setDynamicPainter(painter: Painter, forKey: String) {
        visuals[forKey] = SvgaDynamicVisual.PainterValue(painter)
        revision += 1
    }

    fun setDynamicText(
        text: String,
        forKey: String,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        texts[forKey] = SvgaDynamicText(text, style, overflow, softWrap, maxLines)
        revision += 1
    }

    fun setDynamicDrawer(forKey: String, drawer: SvgaDynamicDrawer) {
        drawers[forKey] = drawer
        revision += 1
    }

    fun setClickArea(forKey: String, onClick: (String) -> Unit) {
        clickHandlers[forKey] = onClick
    }

    fun clearDynamicObjects() {
        hidden.clear()
        visuals.clear()
        texts.clear()
        drawers.clear()
        clickHandlers.clear()
        revision += 1
    }

    internal fun dispatchClick(position: Offset, regions: Map<String, Rect>): Boolean {
        val hit = regions.entries.lastOrNull { (_, rect) -> rect.contains(position) } ?: return false
        clickHandlers[hit.key]?.invoke(hit.key)
        return hit.key in clickHandlers
    }

    internal fun hasClickHandler(key: String): Boolean = key in clickHandlers
}

@Composable
fun rememberSvgaDynamicEntity(): SvgaDynamicEntity = remember { SvgaDynamicEntity() }
