package com.korilin.samples.compose.trace.glide

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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

fun Modifier.glideBackground(
    model: Any?,
    placeholder: Int,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
    listener: PainterRequestListener? = null,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = { Glide.with(it).asDrawable() },
): Modifier = composed {

    val preview = LocalInspectionMode.current
    val context = LocalContext.current

    val painter = when (model) {
        is Painter -> model
        is Int -> if (preview) painterResource(id = model) else null
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

    this.glideBackground(
        nodeModel = nodeModel,
        loadingModel = ResModel(placeholder),
        alignment,
        contentScale,
        alpha,
        colorFilter
    )
}

private fun Modifier.glideBackground(
    nodeModel: GlideNodeModel,
    loadingModel: GlidePlaceholderModel,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
): Modifier = clipToBounds() then GlidePainterElement(
    nodeModel = nodeModel,
    loadingModel = loadingModel,
    failureModel = null,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
)