package munchkin.resources.runtime

import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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

private val mainHandler by lazy(LazyThreadSafetyMode.NONE) {
    Handler(Looper.getMainLooper())
}

internal fun Drawable.toResourcePainter(): Painter = when (this) {
    is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
    is ColorDrawable -> ColorPainter(Color(color))
    else -> ResourceDrawablePainter(mutate())
}

private val Drawable.painterIntrinsicSize: Size
    get() = when {
        intrinsicWidth >= 0 && intrinsicHeight >= 0 -> {
            Size(width = intrinsicWidth.toFloat(), height = intrinsicHeight.toFloat())
        }

        else -> Size.Unspecified
    }

private class ResourceDrawablePainter(
    private val drawable: Drawable
) : Painter(), RememberObserver {

    private var invalidateTick by mutableIntStateOf(0)
    private var painterIntrinsicSize by mutableStateOf(drawable.painterIntrinsicSize)

    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            invalidateTick++
            val newSize = drawable.painterIntrinsicSize
            if (newSize != painterIntrinsicSize) {
                painterIntrinsicSize = newSize
            }
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) {
            mainHandler.postAtTime(what, whenMillis)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            mainHandler.removeCallbacks(what)
        }
    }

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
    }

    override fun onRemembered() {
        drawable.callback = callback
        drawable.setVisible(true, true)
        if (drawable is Animatable) {
            drawable.start()
        }
    }

    override fun onForgotten() {
        dispose()
    }

    override fun onAbandoned() {
        dispose()
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

    override val intrinsicSize: Size
        get() = painterIntrinsicSize

    override fun DrawScope.onDraw() {
        invalidateTick
        drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
        drawIntoCanvas { canvas ->
            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }

    private fun dispose() {
        if (drawable is Animatable) {
            drawable.stop()
        }
        drawable.setVisible(false, false)
        drawable.callback = null
    }
}
