package com.korilin.compose.akit.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

/**
 * Use Glide load background image.
 *
 * [alignment] and [contentScale] will ignore if image type is nine patch.
 */
fun Modifier.glideBackground(
    model: Any?,
    placeholder: Int? = null,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
    listener: PainterRequestListener? = null,
    extension: GlideExtension = GlideExtension.NORMAL,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = { Glide.with(it).asDrawable() },
): Modifier = composed {

    val preview = LocalInspectionMode.current
    val context = LocalContext.current

    val painter = when (model) {
        is Painter -> model
        is Int -> if (preview) {
            val drawable = ContextCompat.getDrawable(context, model)
            drawable!!.toPainter()
        } else null
        else -> null
    }

    val nodeModel = remember(model) {
        if (painter != null) PainterModel(painter)
        else GlideRequestModel(
            model = model,
            requestBuilder = { requestBuilder(context) },
            listener = listener
        )
    }

    val loadingRes = placeholder ?: if (preview) model as? Int? else null

    this.glideBackground(
        nodeModel = nodeModel,
        loadingModel = loadingRes?.let { ResModel(it) },
        alignment,
        contentScale,
        alpha,
        colorFilter,
        extension
    )
}
