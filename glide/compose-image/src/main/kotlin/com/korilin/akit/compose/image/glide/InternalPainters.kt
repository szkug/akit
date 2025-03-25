package com.korilin.akit.compose.image.glide

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

private val MAIN_HANDLER by lazy(LazyThreadSafetyMode.NONE) {
    Handler(Looper.getMainLooper())
}


private val Drawable.intrinsicSize: Size
    get() = when {
        // Only return a finite size if the drawable has an intrinsic size
        intrinsicWidth >= 0 && intrinsicHeight >= 0 -> {
            Size(width = intrinsicWidth.toFloat(), height = intrinsicHeight.toFloat())
        }

        else -> Size.Unspecified
    }

internal fun Drawable.toPainter(): Painter =
    when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        is ColorDrawable -> ColorPainter(Color(color))
        is NinePatchDrawable -> NinePatchPainter(mutate())
        else -> DrawablePainter(mutate())
    }

internal interface AnimatablePainter {
    fun startAnimation()
    fun stopAnimation()
}

internal object EmptyPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified
    override fun DrawScope.onDraw() {}
}

internal abstract class HasPaddingPainter: Painter() {
    val padding = Rect()
}

internal class NinePatchPainter(
    val drawable: Drawable
) : HasPaddingPainter() {

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            // Update the drawable's bounds to match the intrinsic size
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        drawable.getPadding(padding)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        return drawable.setLayoutDirection(
            when (layoutDirection) {
                LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
                LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
            }
        )
    }

    /**
     * Never give a specific size for NinePatch,
     * otherwise drawable will lose the adaptive size.
     */
    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            // Update the Drawable's bounds
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }

    override fun toString(): String {
        return "NinePatchPainter@${hashCode()}(drawable=$drawable, size=${drawable.intrinsicSize})"
    }
}

internal class DrawablePainter(
    private val drawable: Drawable
) : HasPaddingPainter(), RememberObserver, AnimatablePainter {

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            // Update the drawable's bounds to match the intrinsic size
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        drawable.getPadding(padding)
    }

    // Cache the intrinsic size to avoid repeated calculations
    private val initialIntrinsicSize = drawable.intrinsicSize
    
    // Use a more efficient approach for invalidation tracking
    private var drawInvalidateTick by mutableIntStateOf(0)
    
    // Only update intrinsic size when it actually changes
    private var drawableIntrinsicSize by mutableStateOf(initialIntrinsicSize)

    // Lazy initialize callback to avoid unnecessary object creation for static drawables
    private val callback: Drawable.Callback by lazy {
        object : Drawable.Callback {
            override fun invalidateDrawable(d: Drawable) {
                // Update the tick so that we get re-drawn
                drawInvalidateTick++
                
                // Only update intrinsic size if it has changed
                val newSize = drawable.intrinsicSize
                if (newSize != drawableIntrinsicSize) {
                    drawableIntrinsicSize = newSize
                }
            }

            override fun scheduleDrawable(d: Drawable, what: Runnable, time: Long) {
                MAIN_HANDLER.postAtTime(what, time)
            }

            override fun unscheduleDrawable(d: Drawable, what: Runnable) {
                MAIN_HANDLER.removeCallbacks(what)
            }
        }
    }

    override fun startAnimation() {
        // Only set callback if needed
        if (drawable is Animatable || drawable.intrinsicWidth < 0 || drawable.intrinsicHeight < 0) {
            drawable.callback = callback
            drawable.setVisible(true, true)

            if (drawable is Animatable) drawable.start()
        }
    }

    override fun stopAnimation() {
        // Only perform cleanup if we're actually animating
        if (drawable is Animatable) {
            // Don't re-use memory instance if drawable is Webp animation
            // https://github.com/bumptech/glide/issues/5176
            drawable.stop()
        }
        
        // Clear callback to prevent memory leaks
        if (drawable.callback != null) {
            drawable.setVisible(false, false)
            drawable.callback = null
        }
    }

    override fun onRemembered() = startAnimation()

    override fun onAbandoned() = stopAnimation()

    override fun onForgotten() = stopAnimation()

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        return drawable.setLayoutDirection(
            when (layoutDirection) {
                LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
                LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
            }
        )
    }

    // Use cached value to avoid recalculation
    override val intrinsicSize: Size get() = drawableIntrinsicSize

    override fun DrawScope.onDraw() {
        // Reading this ensures that we invalidate when invalidateDrawable() is called
        drawInvalidateTick
        
        // Avoid creating new objects in the draw path
        val width = size.width.roundToInt()
        val height = size.height.roundToInt()
        
        drawIntoCanvas { canvas ->
            // Update the Drawable's bounds
            drawable.setBounds(0, 0, width, height)

            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }

    override fun toString(): String {
        return "DrawablePainter@${hashCode()}(drawable=$drawable, size=$intrinsicSize)"
    }
}
