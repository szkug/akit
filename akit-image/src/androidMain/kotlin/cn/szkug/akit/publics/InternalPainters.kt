package cn.szkug.akit.publics

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import cn.szkug.akit.compose.image.HasPaddingPainter
import cn.szkug.akit.compose.image.ImagePadding
import kotlin.math.roundToInt

private val MAIN_HANDLER by lazy(LazyThreadSafetyMode.NONE) {
    Handler(Looper.getMainLooper())
}

private val Drawable.intrinsicSize: Size
    get() = when {
        intrinsicWidth >= 0 && intrinsicHeight >= 0 -> {
            Size(width = intrinsicWidth.toFloat(), height = intrinsicHeight.toFloat())
        }

        else -> Size.Unspecified
    }

fun Drawable.toPainter(): Painter = when (this) {
    is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
    is ColorDrawable -> ColorPainter(Color(color))
    is NinePatchDrawable -> AndroidNinePatchPainter(mutate())
    else -> DrawablePainter(mutate())
}

interface AnimatablePainter {
    fun startAnimation()
    fun stopAnimation()
}

class AndroidNinePatchPainter(
    private val drawable: Drawable
) : HasPaddingPainter() {

    override val padding: ImagePadding = Rect().let { rect ->
        drawable.getPadding(rect)
        ImagePadding(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
        )
    }

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
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

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}

class DrawablePainter(
    private val drawable: Drawable
) : HasPaddingPainter(), RememberObserver, AnimatablePainter {

    private val paddingRect = Rect()

    override val padding: ImagePadding
        get() = ImagePadding(
            left = paddingRect.left,
            top = paddingRect.top,
            right = paddingRect.right,
            bottom = paddingRect.bottom,
        )

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        drawable.getPadding(paddingRect)
    }

    private val initialIntrinsicSize = drawable.intrinsicSize
    private var drawInvalidateTick by mutableIntStateOf(0)
    private var drawableIntrinsicSize by mutableStateOf(initialIntrinsicSize)

    private val callback: Drawable.Callback by lazy {
        object : Drawable.Callback {
            override fun invalidateDrawable(d: Drawable) {
                drawInvalidateTick++
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
        if (drawable is Animatable || drawable.intrinsicWidth < 0 || drawable.intrinsicHeight < 0) {
            drawable.callback = callback
            drawable.setVisible(true, true)
            if (drawable is Animatable) drawable.start()
        }
    }

    override fun stopAnimation() {
        if (drawable is Animatable) {
            drawable.stop()
        }
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

    override val intrinsicSize: Size get() = drawableIntrinsicSize

    override fun DrawScope.onDraw() {
        drawInvalidateTick
        val width = size.width.roundToInt()
        val height = size.height.roundToInt()

        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, width, height)
            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}
