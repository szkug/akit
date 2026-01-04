package com.korilin.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter

@Composable
internal actual fun PlatformAsyncImage(
    model: Any?,
    placeholder: PainterModel?,
    failureRes: ResourceModel?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alignment: Alignment,
    alpha: Float,
    colorFilter: ColorFilter?,
    context: AsyncImageContext,
) {
    val errorPainter = (failureRes as? PainterModel)?.painter
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder?.painter,
        error = errorPainter,
        fallback = errorPainter,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

internal actual fun Modifier.platformAsyncBackground(
    model: Any?,
    placeholder: PainterModel?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    context: AsyncImageContext?,
): Modifier = composed {
    val errorPainter = placeholder?.painter
    val painter = rememberAsyncImagePainter(
        model = model,
        placeholder = placeholder?.painter,
        error = errorPainter,
        fallback = errorPainter,
        contentScale = contentScale,
    )

    paint(
        painter = painter,
        sizeToIntrinsics = false,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}
