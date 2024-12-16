package com.korilin.compose.akit.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.trace
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

/**
 * Async image load node base on glide.
 *
 * Use sample:
 * ```Kotlin
 * GlideAsyncImage(
 *     model = model,
 *     contentDescription = null,
 *     modifier = Modifier
 *         .height(20.dp)
 *         .wrapContentWidth()
 *         .background(type.color),
 *     contentScale = ContentScale.FillHeight,
 *     requestBuilder = {
 *         GlideApp.with(context)
 *             .asDrawable().diskCacheStrategy(diskCache)
 *             .skipMemoryCache(true)
 *             .multiCache("image")
 *     }
 * )
 * ```
 *
 * @param model Support Url string, Uri, Drawable ResId, or [Painter],
 * but not recommend use painter directly
 * @param tag Log tag used to locate problems
 * @param requestBuilder return custom requestBuilder. The model is automatically loaded at the right time,
 * so don't load model directly in requestBuilder. and use [listener] param if need set RequestListener.
 */
@Composable
fun GlideAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
    loadingModel: Any? = null,
    failureModel: Any? = null,
    listener: PainterRequestListener? = null,
    extension: GlideExtension = GlideExtension.NORMAL,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = { Glide.with(it).asDrawable() },
) = trace("GlideAsyncImage") {

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

    Layout(
        modifier = modifier
            .glidePainterNode(
                nodeModel = nodeModel,
                loadingModel = loadingModel?.castPlaceholderModel(),
                failureModel = failureModel?.castPlaceholderModel(),
                contentDescription,
                alignment,
                contentScale,
                alpha,
                colorFilter,
                extension,
            ),
        measurePolicy = { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        },
    )
}


@Composable
private fun Any?.castPlaceholderModel(): GlidePlaceholderModel? {
    val preview = LocalInspectionMode.current
    return when (this) {
        is Int -> if (preview) PainterModel(painterResource(this)) else ResModel(this)
        is Painter -> PainterModel(this)
        else -> null
    }
}