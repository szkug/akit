package com.korilin.akit.compose.image.publics

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.trace
import com.bumptech.glide.Glide
import com.korilin.akit.compose.image.glide.GlideDefaults
import com.korilin.akit.compose.image.glide.PainterModel
import com.korilin.akit.compose.image.glide.RequestModel
import com.korilin.akit.compose.image.glide.ResModel
import com.korilin.akit.compose.image.glide.glidePainterNode
import com.korilin.akit.compose.image.glide.toPainter

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
    placeholder: Int? = null,
    failureRes: Int? = null,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
    extension: AsyncImageContext = rememberAsyncImageContext(),
) = trace("GlideAsyncImage") {

    Layout(
        modifier = modifier
            .glidePainterNode(
                requestModel = RequestModel(model),
                placeholderModel = PainterModel.fromId(placeholder, LocalContext.current),
                failureModel = failureRes?.let { ResModel(it) },
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