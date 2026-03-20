package munchkin.resources.runtime

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.flow.Flow
import munchkin.graph.renderscript.BlurConfig
import kotlin.jvm.JvmInline

interface RuntimeImageLoadListener {
    fun onStart(model: Any?) {}
    fun onSuccess(model: Any?) {}
    fun onFailure(model: Any?, exception: Throwable) {}
    fun onCancel(model: Any?) {}
}

data class RuntimeImageSizeLimit(
    val maxWidth: Int = 0,
    val maxHeight: Int = 0,
)

interface RuntimeImageRequestContext {
    val logger: MunchkinLogger
    val listener: RuntimeImageLoadListener?
    val ignoreImagePadding: Boolean
    val animationIterations: Int
    val blurConfig: BlurConfig?
    val sizeLimit: RuntimeImageSizeLimit?
    val supportNinepatch: Boolean
    val supportLottie: Boolean
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

sealed interface ImageAsyncLoadResult<out T : ImageAsyncLoadData> {
    @JvmInline
    value class Error<T : ImageAsyncLoadData>(val data: T?) : ImageAsyncLoadResult<T>

    @JvmInline
    value class Success<T : ImageAsyncLoadData>(val data: T) : ImageAsyncLoadResult<T>

    @JvmInline
    value class Cleared<T : ImageAsyncLoadData>(val data: T?) : ImageAsyncLoadResult<T>
}

interface RuntimeImageRequestEngine<C : RuntimeEngineContext, out Data : ImageAsyncLoadData> : RuntimeRequestEngine<C> {

    val engineSizeOriginal: Int

    suspend fun flowRequest(
        engineContext: C,
        imageContext: RuntimeImageRequestContext,
        size: ResolvableImageSize,
        contentScale: ContentScale,
        requestModel: RequestModel,
        failureModel: ResourceModel?,
    ): Flow<ImageAsyncLoadResult<Data>>
}
