package cn.szkug.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale

/**
 * Async image load node base on platform-specific image loader.
 */
@Composable
fun AkitAsyncImage(
    model: Any?,
    placeholder: PainterModel? = null,
    failureRes: ResourceModel? = null,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = AsyncImageDefaults.DefaultContentScale,
    alignment: Alignment = AsyncImageDefaults.DefaultAlignment,
    alpha: Float = AsyncImageDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = AsyncImageDefaults.DefaultColorFilter,
    context: AsyncImageContext = rememberAsyncImageContext(),
) {
    PlatformAsyncImage(
        model = model,
        placeholder = placeholder,
        failureRes = failureRes,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
        context = context,
    )
}
