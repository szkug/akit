package munchkin.svga

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Describes a runtime visual replacement for one SVGA sprite slot.
 */
sealed interface SvgaDynamicVisual {
    /**
     * Replaces the original sprite content with a bitmap.
     */
    data class Bitmap(
        /** Bitmap used as the replacement content for the sprite. */
        val image: ImageBitmap,
    ) : SvgaDynamicVisual

    /**
     * Replaces the original sprite content with a Compose painter.
     */
    data class PainterValue(
        /** Painter used as the replacement content for the sprite. */
        val painter: Painter,
    ) : SvgaDynamicVisual
}

/**
 * Stores text replacement metadata for one dynamic text slot.
 */
data class SvgaDynamicText(
    /** Resolved text content rendered into the target slot. */
    val text: String,
    /** Compose text style used to render the replacement text. */
    val style: TextStyle = TextStyle.Default,
    /** Overflow behavior forwarded to the text layout. */
    val overflow: TextOverflow = TextOverflow.Clip,
    /** Whether soft wrapping is enabled for the replacement text. */
    val softWrap: Boolean = true,
    /** Maximum line count allowed for the replacement text. */
    val maxLines: Int = Int.MAX_VALUE,
)

/**
 * Provides the draw callback with the sprite key, current frame, and layout bounds.
 */
data class SvgaDynamicDrawContext(
    /** Sprite key currently being customized. */
    val key: String,
    /** Current frame index at draw time. */
    val frameIndex: Int,
    /** Layout rect resolved from the movie for the current sprite instance. */
    val layout: SvgaLayout,
)

/**
 * Draw callback used to inject custom rendering into a sprite slot.
 *
 * Returning `true` means the callback fully handled the draw and the built-in renderer should stop.
 * Returning `false` means the built-in renderer may continue drawing the original content.
 */
typealias SvgaDynamicDrawer = androidx.compose.ui.graphics.drawscope.DrawScope.(SvgaDynamicDrawContext) -> Boolean

/**
 * Collects all dynamic overrides that can be applied to one SVGA playback session.
 *
 * Business-facing responsibilities:
 * - replace images, painters, and text content by sprite key
 * - hide selected sprites
 * - inject custom draw callbacks
 * - dispatch click actions for hit-tested sprite regions
 */
@Stable
class SvgaDynamicEntity {
    /** Hidden-state overrides keyed by sprite name. */
    internal val hidden = mutableStateMapOf<String, Boolean>()

    /** Visual replacements keyed by sprite name. */
    internal val visuals = mutableStateMapOf<String, SvgaDynamicVisual>()

    /** Text replacements keyed by sprite name. */
    internal val texts = mutableStateMapOf<String, SvgaDynamicText>()

    /** Custom draw callbacks keyed by sprite name. */
    internal val drawers = mutableStateMapOf<String, SvgaDynamicDrawer>()

    /** Click handlers keyed by sprite name for runtime hit testing. */
    private val clickHandlers = mutableMapOf<String, (String) -> Unit>()

    /** Monotonic revision used by the renderer to observe dynamic-content changes. */
    internal var revision by mutableIntStateOf(0)
        private set

    /**
     * Marks a sprite as hidden or visible.
     */
    fun setHidden(value: Boolean, forKey: String) {
        hidden[forKey] = value
        revision += 1
    }

    /**
     * Replaces a sprite with a bitmap.
     */
    fun setDynamicImage(bitmap: ImageBitmap, forKey: String) {
        visuals[forKey] = SvgaDynamicVisual.Bitmap(bitmap)
        revision += 1
    }

    /**
     * Replaces a sprite with a painter.
     */
    fun setDynamicPainter(painter: Painter, forKey: String) {
        visuals[forKey] = SvgaDynamicVisual.PainterValue(painter)
        revision += 1
    }

    /**
     * Replaces a text slot with runtime text styling information.
     */
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

    /**
     * Registers a custom draw callback for one sprite slot.
     */
    fun setDynamicDrawer(forKey: String, drawer: SvgaDynamicDrawer) {
        drawers[forKey] = drawer
        revision += 1
    }

    /**
     * Registers a click callback for a hit-tested sprite region.
     */
    fun setClickArea(forKey: String, onClick: (String) -> Unit) {
        clickHandlers[forKey] = onClick
        revision += 1
    }

    /**
     * Clears every dynamic override from this entity.
     */
    fun clearDynamicObjects() {
        hidden.clear()
        visuals.clear()
        texts.clear()
        drawers.clear()
        clickHandlers.clear()
        revision += 1
    }

    /**
     * Dispatches one pointer position to the top-most hit region.
     */
    internal fun dispatchClick(position: Offset, regions: Map<String, Rect>): Boolean {
        val hit = regions.entries.lastOrNull { (_, rect) -> rect.contains(position) } ?: return false
        clickHandlers[hit.key]?.invoke(hit.key)
        return hit.key in clickHandlers
    }

    /**
     * Reports whether the given sprite key currently owns a click handler.
     */
    internal fun hasClickHandler(key: String): Boolean = key in clickHandlers

    companion object {
        /**
         * Shared immutable-like empty instance used when the caller does not need dynamic overrides.
         */
        val EMPTY = SvgaDynamicEntity()
    }
}
