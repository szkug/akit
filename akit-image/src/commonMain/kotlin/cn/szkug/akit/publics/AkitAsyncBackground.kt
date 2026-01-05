package cn.szkug.akit.publics

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale

/**
 * Use platform-specific image loader to draw background image.
 */
fun Modifier.akitAsyncBackground(
    model: Any?,
    placeholder: PainterModel? = null,
    alignment: Alignment = AsyncImageDefaults.DefaultAlignment,
    contentScale: ContentScale = AsyncImageDefaults.DefaultContentScale,
    alpha: Float = AsyncImageDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = AsyncImageDefaults.DefaultColorFilter,
    context: AsyncImageContext? = null,
): Modifier = platformAsyncBackground(
    model = model,
    placeholder = placeholder,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    context = context,
)
