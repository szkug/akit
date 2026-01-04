package com.korilin.akit.compose.image.publics

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.korilin.akit.compose.image.glide.GlideDefaults
import com.korilin.akit.compose.image.glide.PainterModel
import com.korilin.akit.compose.image.glide.RequestModel
import com.korilin.akit.compose.image.glide.glideBackground


/**
 * Use Glide load background image.
 *
 * [alignment] and [contentScale] will ignore if image type is nine patch.
 */
fun Modifier.glideBackground(
    model: Any?,
    placeholder: PainterModel? = null,
    alignment: Alignment = GlideDefaults.DefaultAlignment,
    contentScale: ContentScale = GlideDefaults.DefaultContentScale,
    alpha: Float = GlideDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = GlideDefaults.DefaultColorFilter,
    context: AsyncImageContext? = null,
): Modifier = composed {
    this.glideBackground(
        requestModel = RequestModel(model),
        placeholderModel = placeholder,
        alignment,
        contentScale,
        alpha,
        colorFilter,
        extension = context ?: rememberAsyncImageContext()
    )
}
