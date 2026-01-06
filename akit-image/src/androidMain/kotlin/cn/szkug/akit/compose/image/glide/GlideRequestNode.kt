package cn.szkug.akit.compose.image.glide

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import cn.szkug.akit.compose.image.AsyncRequestNode
import cn.szkug.akit.compose.image.RequestModel
import cn.szkug.akit.publics.AndroidAsyncImageContextData
import cn.szkug.akit.publics.AnimatablePainter
import cn.szkug.akit.publics.AsyncImageContext
import cn.szkug.akit.publics.PainterModel
import cn.szkug.akit.publics.ResIdModel
import cn.szkug.akit.publics.ResourceModel
import cn.szkug.akit.publics.toPainter
import cn.szkug.graphics.ninepatch.BitmapType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

private const val TRACE_SECTION_NAME = "GlideRequestNode"

internal abstract class GlideRequestNode(
    requestModel: RequestModel,
    placeholderModel: PainterModel?,
    failureModel: ResourceModel?,
    context: AsyncImageContext,
    contentScale: ContentScale,
) : AsyncRequestNode<GlideLoadResult<Drawable>>(
    requestModel = requestModel,
    placeholderModel = placeholderModel,
    failureModel = failureModel,
    context = context,
    contentScale = contentScale,
) {

    // Shared extension functions
    protected fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    protected fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    protected fun Constraints.inferredGlideSize(): Size {
        val width = if (hasBoundedWidth) {
            maxWidth
        } else {
            com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
        }
        val height =
            if (hasBoundedHeight) {
                maxHeight
            } else {
                com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
            }
        return Size(width.toFloat(), height.toFloat())
    }

    override fun log(subtag: String?, message: () -> String) {
        if (context.enableLog) GlideDefaults.logger.info(TRACE_SECTION_NAME) {
            if (subtag == null) message()
            else "[$subtag] ${message()}"
        }
    }

    protected val glideSize: ResolvableGlideSize = AsyncGlideSize()

    private val androidData: AndroidAsyncImageContextData
        get() = context.platformData as? AndroidAsyncImageContextData
            ?: error("AndroidAsyncImageContextData is missing.")

    override fun resolveFailurePainter(): Painter? {
        return when (val model = currentFailureModel()) {
            is PainterModel -> model.painter
            is DrawableModel -> model.drawable.toPainter()
            is ResIdModel -> AppCompatResources.getDrawable(androidData.context, model.resId)?.toPainter()
            else -> null
        }
    }

    override fun resolvePainter(
        result: GlideLoadResult<Drawable>,
        failurePainter: Painter?,
        placeholderPainter: Painter,
    ): Painter {
        return when (result) {
            is GlideLoadResult.Error -> result.drawable?.toPainter() ?: failurePainter ?: placeholderPainter
            is GlideLoadResult.Success -> result.drawable.toPainter()
            is GlideLoadResult.Cleared -> placeholderPainter
        }
    }

    override fun onSetup() {
        val current = painter
        if (current is AnimatablePainter) current.startAnimation()
    }

    override fun onPainterChanged(old: Painter, new: Painter) {
        if (old is AnimatablePainter) old.stopAnimation()
        if (new is AnimatablePainter) new.startAnimation()
    }

    override fun onStopRequest() {
        if (!glideSize.sizeReady()) glideSize.putSize(Size.Unspecified)
    }

    override fun flowRequest(requestModel: RequestModel): Flow<GlideLoadResult<Drawable>> = flow {
        val model = requestModel.model ?: return@flow emit(GlideLoadResult.Cleared(null))

        val size = glideSize.awaitSize()
        val cacheKey = buildCacheKey(model, size)
        val cache = AndroidImageMemoryCache.get(androidData.context)

        val cached = cache.get(cacheKey)
        if (cached != null) {
            emit(GlideLoadResult.Success(cached))
            return@flow
        }

        val drawable = withContext(Dispatchers.IO) {
            loadDrawableForModel(androidData.context, model, size)
        }

        if (drawable != null) {
            cache.put(cacheKey, drawable)
            emit(GlideLoadResult.Success(drawable))
        } else {
            emit(GlideLoadResult.Error(null))
        }
    }

    private fun buildCacheKey(model: Any, size: GlideSize): String {
        val modelKey = when (model) {
            is Int -> "res:$model"
            is File -> "file:${model.absolutePath}"
            is Uri -> "uri:$model"
            else -> model.toString()
        }
        return "$modelKey|${size.width}x${size.height}|$contentScale"
    }

    private fun loadDrawableForModel(
        context: Context,
        model: Any,
        size: GlideSize,
    ): Drawable? {
        if (model is Int) {
            return AppCompatResources.getDrawable(context, model)
        }

        val file = when (model) {
            is File -> model
            else -> downloadFile(context, model) ?: return null
        }

        if (isNinePatchCandidate(file)) {
            val bitmap = decodeBitmap(file) ?: return null
            val type = BitmapType.determineBitmapType(bitmap)
            if (type == BitmapType.RawNinePatch || type == BitmapType.NinePatch) {
                return type.createNinePatchDrawable(context.resources, bitmap, file.name)
                    ?: BitmapDrawable(context.resources, bitmap)
            }
        }

        return loadDrawableFromFile(context, file, size) ?: decodeBitmap(file)?.let {
            BitmapDrawable(context.resources, it)
        }
    }

    private fun loadDrawableFromFile(
        context: Context,
        file: File,
        size: GlideSize,
    ): Drawable? {
        val request = androidData.requestBuilder(context)
            .load(file)
            .setupTransforms(contentScale, androidData)
            .applySize(size)

        val future = request.submit()
        return try {
            future.get()
        } catch (e: Exception) {
            log("loadDrawable") { "${file.absolutePath} ${e.message}" }
            null
        } finally {
            com.bumptech.glide.Glide.with(context).clear(future)
        }
    }

    private fun downloadFile(context: Context, model: Any): File? {
        val request = com.bumptech.glide.Glide.with(context)
            .downloadOnly()
            .load(model)
        val future = request.submit()
        return try {
            future.get()
        } catch (e: Exception) {
            log("downloadFile") { "${model} ${e.message}" }
            null
        } finally {
            com.bumptech.glide.Glide.with(context).clear(future)
        }
    }

    private fun isNinePatchCandidate(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".9.png") || name.endsWith(".9.webp")
    }

    private fun decodeBitmap(file: File): android.graphics.Bitmap? {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
}

private fun com.bumptech.glide.RequestBuilder<Drawable>.applySize(size: GlideSize): com.bumptech.glide.RequestBuilder<Drawable> {
    val width = size.width
    val height = size.height
    return if (width == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL &&
        height == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
    ) {
        this
    } else {
        override(width, height)
    }
}
