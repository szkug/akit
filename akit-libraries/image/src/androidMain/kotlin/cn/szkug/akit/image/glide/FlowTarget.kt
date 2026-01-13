package cn.szkug.akit.image.glide

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import cn.szkug.akit.image.AsyncLoadResult
import cn.szkug.akit.image.ImageSize
import cn.szkug.akit.image.AsyncImageContext
import cn.szkug.akit.image.ResolvableImageSize
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.manager
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private fun Drawable.toDrawableAsyncLoadData() = DrawableAsyncLoadData(this)

internal fun flowOfId(
    context: AsyncImageContext,
    resId: Int,
): Flow<AsyncLoadResult<DrawableAsyncLoadData>> {
    return flow {
        val drawable = AppCompatResources.getDrawable(context.context, resId)!!
        val result = AsyncLoadResult.Success(drawable.toDrawableAsyncLoadData())
        emit(result)
    }
}

internal fun RequestBuilder<Drawable>.flowDrawableOfSize(
    context: AsyncImageContext,
    size: ResolvableImageSize,
): Flow<AsyncLoadResult<DrawableAsyncLoadData>> {
    return callbackFlow {
        val target = DrawableFlowTarget(context, this, size)
        // use add listener
        addListener(target).into(target)
        awaitClose { manager.clear(target) }
    }
}

private class DrawableFlowTarget(
    private val context: AsyncImageContext,
    private val scope: ProducerScope<AsyncLoadResult<DrawableAsyncLoadData>>,
    private val size: ResolvableImageSize,
) : Target<Drawable>, RequestListener<Drawable> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean
    ): Boolean {
        context.logger.error("FlowTarget", e)
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        context.logger.info("FlowTarget") {
            "onResourceReady first:$isFirstResource source:$dataSource model:$model"
        }

        return false
    }

    override fun getSize(cb: SizeReadyCallback) {
        scope.launch(Dispatchers.IO) {
            val complete = size.awaitSize()
            context.logger.debug("FlowTarget") { "getSize $complete" }
            cb.onSizeReady(complete.width, complete.height)
        }
    }

    @Volatile
    var currentRequest: Request? = null
    override fun setRequest(request: Request?) {
        currentRequest = request
    }

    override fun getRequest(): Request? {
        return currentRequest
    }

    override fun removeCallback(cb: SizeReadyCallback) {}
    override fun onLoadCleared(placeholder: Drawable?) {
        scope.trySend(AsyncLoadResult.Cleared(placeholder?.toDrawableAsyncLoadData()))
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        scope.trySend(AsyncLoadResult.Error(errorDrawable?.toDrawableAsyncLoadData()))
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        scope.trySend(AsyncLoadResult.Success(resource.toDrawableAsyncLoadData()))
    }

    override fun onStart() {}
    override fun onStop() {}
    override fun onDestroy() {}
    override fun onLoadStarted(placeholder: Drawable?) {}
}
