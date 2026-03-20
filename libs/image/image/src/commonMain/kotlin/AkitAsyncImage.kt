package munchkin.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import munchkin.graph.lottie.LottieResource
import munchkin.graph.lottie.rememberLottiePainter
import munchkin.resources.loader.ImageAsyncRequestEngine
import munchkin.resources.runtime.PaintableResourceId
import munchkin.resources.runtime.painterResource

/**
 * Async image load node base on platform-specific image loader.
 */
@Composable
fun <C : EngineContext, Data : AsyncLoadData> MunchkinAsyncImage(
    model: Any?,
    placeholder: Any? = null,
    failureRes: Any? = null,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = AsyncImageDefaults.DefaultContentScale,
    alignment: Alignment = AsyncImageDefaults.DefaultAlignment,
    alpha: Float = AsyncImageDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = AsyncImageDefaults.DefaultColorFilter,
    context: AsyncImageContext = rememberAsyncImageContext(),
    engine: AsyncRequestEngine<C, Data>,
) {
    Layout(
        modifier = modifier
            .asyncPainterNode(
                requestModel = RequestModel(model),
                placeholderModel = placeholder.toPainterModel(),
                failureModel = failureRes.toResourceModel(),
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                imageContext = context,
                engineContext = LocalEngineContextRegister.resolve(engine),
                engine = engine
            ),
        measurePolicy = { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        },
    )
}

/**
 * Use platform-specific image loader to draw background image.
 */
@Composable
fun <C : EngineContext, Data : AsyncLoadData> Modifier.munchkinAsyncBackground(
    model: Any?,
    placeholder: Any? = model,
    alignment: Alignment = AsyncImageDefaults.DefaultAlignment,
    contentScale: ContentScale = AsyncImageDefaults.DefaultContentScale,
    alpha: Float = AsyncImageDefaults.DefaultAlpha,
    colorFilter: ColorFilter? = AsyncImageDefaults.DefaultColorFilter,
    context: AsyncImageContext = rememberAsyncImageContext(supportNinepatch = true),
    engine: AsyncRequestEngine<C, Data>,
): Modifier = composed {

    asyncBackgroundNode(
        requestModel = RequestModel(model),
        placeholderModel = placeholder.toPainterModel(),
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        imageContext = context,
        engineContext = LocalEngineContextRegister.resolve(engine),
        engine = engine
    )
}

@Composable
private fun Any?.toCommonPainterModel(): PainterModel? = when (this) {
    is PaintableResourceId -> PainterModel(painterResource(this))
    is Painter -> PainterModel(this)
    is ImageBitmap -> remember(this) { PainterModel(BitmapPainter(this)) }
    is LottieResource -> PainterModel(rememberLottiePainter(this))
    else -> null
}

@Composable
private fun Any?.toResourceModel(): ResourceModel? = platformResourceModel() ?: toCommonPainterModel()

@Composable
private fun Any?.toPainterModel(): PainterModel? = platformPainterModel() ?: toCommonPainterModel()
