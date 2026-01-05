package cn.szkug.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.trace
import cn.szkug.akit.compose.image.RequestModel
import cn.szkug.akit.compose.image.coil.coilBackground
import cn.szkug.akit.compose.image.coil.coilPainterNode
import coil3.compose.LocalPlatformContext

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
) = trace("AkitAsyncImage") {
    val platformContext = LocalPlatformContext.current
    val resolvedContext = if (context.platformData is IosAsyncImageContextData) {
        context
    } else {
        context.withIosData(rememberIosAsyncImageContextData(platformContext))
    }

    Layout(
        modifier = modifier
            .coilPainterNode(
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
    val platformContext = LocalPlatformContext.current
    val baseContext = context ?: rememberAsyncImageContext()
    val resolvedContext = if (baseContext.platformData is IosAsyncImageContextData) {
        baseContext
    } else {
        baseContext.withIosData(rememberIosAsyncImageContextData(platformContext))
    }

    coilBackground(
        requestModel = RequestModel(model),
        placeholderModel = placeholder,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        extension = resolvedContext,
    )
}
