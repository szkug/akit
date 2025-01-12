package com.korilin.compose.akit.image.publics

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.korilin.compose.akit.image.glide.GlideDefaults
import com.korilin.compose.akit.image.glide.GlideRequestModel
import com.korilin.compose.akit.image.glide.PainterModel
import com.korilin.compose.akit.image.glide.glideBackground
import com.korilin.compose.akit.image.glide.toPainter


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
    context: AsyncImageContext? = null,
): Modifier = composed {
    this.glideBackground(
        requestModel = GlideRequestModel(model),
        placeholderModel = placeholder?.let { PainterModel(painterResource(it)) },
        alignment,
        contentScale,
        alpha,
        colorFilter,
        extension = context ?: rememberAsyncImageContext()
    )
}
