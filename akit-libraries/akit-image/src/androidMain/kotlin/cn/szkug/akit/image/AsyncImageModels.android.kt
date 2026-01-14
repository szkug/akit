package cn.szkug.akit.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
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
actual fun Any?.toResourceModel(): ResourceModel? {
    val context = LocalContext.current
    return remember(this) {
        when (this) {
            is Int -> ResIdModel(this)
            is Drawable -> DrawableModel(this)
            is Bitmap -> DrawableModel(toDrawable(context.resources))
            is Painter -> PainterModel(this)
            else -> null
        }
    }
}

@Composable
actual fun Any?.toPainterModel(): PainterModel? {
    val context = LocalContext.current
    return remember(this) {
        val painter = when (this) {
            is Int -> return@remember PainterModel.fromId(this, context)
            is ImageBitmap -> BitmapPainter(this)
            is Drawable -> toPainter()
            is Bitmap -> toDrawable(context.resources).toPainter()
            is Painter -> this
            else -> null
        }
        painter?.let { PainterModel(it) }
    }
}