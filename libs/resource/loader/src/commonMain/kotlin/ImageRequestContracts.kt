package munchkin.resources.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.flow.Flow
import munchkin.graph.renderscript.BlurConfig
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

interface AsyncImageLoadListener {
    fun onStart(model: Any?) {}
    fun onSuccess(model: Any?) {}
    fun onFailure(model: Any?, exception: Throwable) {}
    fun onCancel(model: Any?) {}
}

data class AsyncImageSizeLimit(
    val maxWidth: Int = 0,
    val maxHeight: Int = 0,
)

class AsyncImageContext(
    val coroutineContext: CoroutineContext,
    val logger: MunchkinLogger = DefaultPlatformMunchkinLogger,
    val listener: AsyncImageLoadListener? = null,
    val ignoreImagePadding: Boolean = false,
    val animationIterations: Int = -1,
    val blurConfig: BlurConfig? = null,
    val sizeLimit: AsyncImageSizeLimit? = null,
    val supportNinepatch: Boolean = false,
    val supportLottie: Boolean = false,
) {
    companion object
}

@Composable
fun rememberAsyncImageContext(
    ignoreImagePadding: Boolean = false,
    logger: MunchkinLogger = DefaultPlatformMunchkinLogger,
    listener: AsyncImageLoadListener? = null,
    animationContext: CoroutineContext = rememberCoroutineScope().coroutineContext,
    blurConfig: BlurConfig? = null,
    sizeLimit: AsyncImageSizeLimit? = null,
    supportNinepatch: Boolean = false,
    vararg keys: Any?,
): AsyncImageContext {
    return remember(ignoreImagePadding, supportNinepatch, animationContext, blurConfig, sizeLimit, *keys) {
        AsyncImageContext(
            ignoreImagePadding = ignoreImagePadding,
            logger = logger,
            listener = listener,
            coroutineContext = animationContext,
            blurConfig = blurConfig,
            sizeLimit = sizeLimit,
            supportNinepatch = supportNinepatch,
            supportLottie = false,
        )
    }
}

interface ResourceModel

@JvmInline
value class PainterModel(val painter: Painter) : ResourceModel {
    override fun toString(): String = "PainterModel($painter)"

    companion object
}

@JvmInline
value class ResIdModel(val resId: Int) : ResourceModel {
    override fun toString(): String = "ResIdModel($resId)"
}

@JvmInline
value class PathModel(val path: Int) : ResourceModel {
    override fun toString(): String = "PathModel($path)"
}

@JvmInline
value class RequestModel(val model: Any?) {
    override fun toString(): String = "AsyncImageRequestModel($model)"
}

interface ImageAsyncLoadData {
    fun painter(): Painter
}

sealed interface ImageAsyncLoadResult<T : ImageAsyncLoadData> {
    @JvmInline
    value class Error<T : ImageAsyncLoadData>(val data: T?) : ImageAsyncLoadResult<T>

    @JvmInline
    value class Success<T : ImageAsyncLoadData>(val data: T) : ImageAsyncLoadResult<T>

    @JvmInline
    value class Cleared<T : ImageAsyncLoadData>(val data: T?) : ImageAsyncLoadResult<T>
}

interface ImageAsyncRequestEngine<Data : ImageAsyncLoadData> {

    val engineSizeOriginal: Int

    suspend fun flowRequest(
        engineContext: EngineContext,
        imageContext: AsyncImageContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<ImageAsyncLoadResult<Data>>
}
