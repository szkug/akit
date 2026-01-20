package cn.szkug.akit.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import cn.szkug.akit.graph.toPainter


fun PainterModel.Companion.fromId(id: Int?, context: Context): PainterModel? {
    if (id == null) return null
    val drawable = AppCompatResources.getDrawable(context, id) ?: return null
    return PainterModel(drawable.toPainter())
}

@Composable
fun PainterModel.Companion.fromId(id: Int?): PainterModel? {
    if (id == null) return null
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, id) ?: return null
    return PainterModel(drawable.toPainter())
}


@Composable
actual fun Any?.platformResourceModel(): ResourceModel? {
    val context = LocalContext.current
    return remember(this) {
        when (this) {
            is Int -> ResIdModel(this)
            is Drawable -> DrawableModel(this)
            is Bitmap -> DrawableModel(toDrawable(context.resources))
            else -> null
        }
    }
}

@Composable
actual fun Any?.platformPainterModel(): PainterModel? {
    val context = LocalContext.current
    return remember(this) {
        val painter = when (this) {
            is Int -> return@remember PainterModel.fromId(this, context)
            is Drawable -> toPainter()
            is Bitmap -> toDrawable(context.resources).toPainter()
            else -> null
        }
        painter?.let { PainterModel(it) }
    }
}
