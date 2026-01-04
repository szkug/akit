package com.korilin.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.trace
import com.korilin.akit.compose.image.glide.RequestModel
import com.korilin.akit.compose.image.glide.glideBackground
import com.korilin.akit.compose.image.glide.glidePainterNode

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
) = trace("GlideAsyncImage") {
    val localContext = LocalContext.current
    val resolvedContext = if (context.platformData is AndroidAsyncImageContextData) {
        context
    } else {
        context.withAndroidData(context.requireAndroidData(localContext))
    }

    Layout(
        modifier = modifier
            .glidePainterNode(
                requestModel = RequestModel(model),
                placeholderModel = placeholder,
                failureModel = failureRes,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                context = resolvedContext,
            ),
        measurePolicy = { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        },
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
    val localContext = LocalContext.current
    val baseContext = context
        ?: rememberAsyncImageContext(requestBuilder = AsyncImageContext.NormalGlideRequestBuilder)
    val resolvedContext = if (baseContext.platformData is AndroidAsyncImageContextData) {
        baseContext
    } else {
        baseContext.withAndroidData(baseContext.requireAndroidData(localContext))
    }

    glideBackground(
        requestModel = RequestModel(model),
        placeholderModel = placeholder,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        extension = resolvedContext,
    )
}
