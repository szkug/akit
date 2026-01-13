package cn.szkug.samples.compose.trace

import android.graphics.Rect
import android.graphics.drawable.NinePatchDrawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat


val Dp.sp @Composable get() = with(LocalDensity.current) { toSp() }

val Dp.px @Composable get() = with(LocalDensity.current) { toPx() }

fun Modifier.draw9Patch(
    @DrawableRes ninePatchRes: Int,
) = composed {
    val context = LocalContext.current
    drawBehind {
        drawIntoCanvas {
            val ninePatch = ContextCompat.getDrawable(context, ninePatchRes)!! as NinePatchDrawable
            ninePatch.run {
                bounds = Rect(0, 0, size.width.toInt(), size.height.toInt())
                draw(it.nativeCanvas)
            }
        }
    }
}