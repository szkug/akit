package cn.szkug.akit.compose.image.coil

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import cn.szkug.akit.compose.image.AsyncRequestNode
import cn.szkug.akit.compose.image.ImageBitmapNinePatchImage
import cn.szkug.akit.compose.image.NinePatchPainter
import cn.szkug.akit.compose.image.RequestModel
import cn.szkug.akit.publics.AsyncImageContext
import cn.szkug.akit.publics.PainterModel
import cn.szkug.akit.publics.ResourceModel
import cn.szkug.akit.publics.requireIosData
import cn.szkug.graphics.ninepatch.NinePatchChunk
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

private const val TRACE_SECTION_NAME = "CoilRequestNode"

internal abstract class CoilRequestNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    failureModel: ResourceModel?,
    context: AsyncImageContext,
    contentScale: ContentScale,
) : AsyncRequestNode<CoilLoadResult<IosCachedImage>>(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = failureModel,
    context = context,
    contentScale = contentScale,
) {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    protected fun Constraints.inferredCoilSize(): Size {
        val width = if (hasBoundedWidth) {
            maxWidth
        } else {
            SIZE_ORIGINAL
        }
        val height =
            if (hasBoundedHeight) {
                maxHeight
            } else {
                SIZE_ORIGINAL
            }
        return Size(width.toFloat(), height.toFloat())
    }

    override fun log(subtag: String?, message: () -> String) {
        if (!context.enableLog) return
        val prefix = if (subtag == null) "" else "[$subtag] "
        println("$TRACE_SECTION_NAME $prefix${message()}")
    }

    protected val coilSize: ResolvableCoilSize = AsyncCoilSize()

    private val iosData get() = context.requireIosData()

    override fun resolvePainter(
        result: CoilLoadResult<IosCachedImage>,
        failurePainter: Painter?,
        placeholderPainter: Painter,
    ): Painter {
        return when (result) {
            is CoilLoadResult.Error -> failurePainter ?: placeholderPainter
            is CoilLoadResult.Success -> result.value.toPainter()
            is CoilLoadResult.Cleared -> placeholderPainter
        }
    }

    override fun onStopRequest() {
        if (!coilSize.sizeReady()) coilSize.putSize(Size.Unspecified)
    }

    override fun flowRequest(requestModel: RequestModel): Flow<CoilLoadResult<IosCachedImage>> = flow {
        val model = requestModel.model ?: return@flow emit(CoilLoadResult.Cleared(Unit))

        val size = coilSize.awaitSize()
        val cacheKey = buildCacheKey(model, size)
        val cache = iosData.memoryCache

        val cached = cache.get(cacheKey)
        if (cached != null) {
            emit(CoilLoadResult.Success(cached))
            return@flow
        }

        val image = withContext(Dispatchers.IO) {
            loadImage(iosData.platformContext, iosData.imageLoader, model)
        }

        if (image != null) {
            cache.put(cacheKey, image)
            emit(CoilLoadResult.Success(image))
        } else {
            emit(CoilLoadResult.Error(null))
        }
    }

    private fun buildCacheKey(model: Any, size: CoilSize): String {
        return "${model}|${size.width}x${size.height}|$contentScale"
    }

    private suspend fun loadImage(
        platformContext: PlatformContext,
        imageLoader: ImageLoader,
        model: Any,
    ): IosCachedImage? {
        val request = when (model) {
            is ImageRequest -> model
            else -> ImageRequest.Builder(platformContext)
                .data(model)
                .build()
        }

        return try {
            when (val result = imageLoader.execute(request)) {
                is SuccessResult -> {
                    val bitmap = result.image.toImageBitmap()
                    val ninePatchImage = ImageBitmapNinePatchImage(bitmap)
                    if (NinePatchChunk.isRawNinePatchImage(ninePatchImage)) {
                        val chunk = NinePatchChunk.createChunkFromRawImage(ninePatchImage)
                        val cropped = cropNinePatch(bitmap)
                        IosCachedImage(cropped, chunk)
                    } else {
                        IosCachedImage(bitmap, null)
                    }
                }
                is ErrorResult -> {
                    log("loadImage") { "${model} ${result.throwable?.message}" }
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            log("loadImage") { "${model} ${e.message}" }
            null
        }
    }

    private fun cropNinePatch(image: ImageBitmap): ImageBitmap {
        val contentWidth = (image.width - 2).coerceAtLeast(1)
        val contentHeight = (image.height - 2).coerceAtLeast(1)
        val out = ImageBitmap(
            width = contentWidth,
            height = contentHeight,
        )
        val canvas = Canvas(out)
        val paint = Paint()
        val src = androidx.compose.ui.geometry.Rect(
            left = 1f,
            top = 1f,
            right = (image.width - 1).toFloat(),
            bottom = (image.height - 1).toFloat(),
        )
        val dst = androidx.compose.ui.geometry.Rect(
            left = 0f,
            top = 0f,
            right = contentWidth.toFloat(),
            bottom = contentHeight.toFloat(),
        )
        canvas.drawImageRect(image, src, dst, paint)
        return out
    }

    private fun IosCachedImage.toPainter(): Painter {
        return if (chunk != null) {
            NinePatchPainter(image, chunk)
        } else {
            BitmapPainter(image)
        }
    }

    private fun Image.toImageBitmap(): ImageBitmap {
        val width = width.coerceAtLeast(1)
        val height = height.coerceAtLeast(1)
        val out = ImageBitmap(width, height)
        val canvas = Canvas(out)
        @Suppress("UNCHECKED_CAST")
        val nativeCanvas = canvas.nativeCanvas as coil3.Canvas
        draw(nativeCanvas)
        return out
    }
}
