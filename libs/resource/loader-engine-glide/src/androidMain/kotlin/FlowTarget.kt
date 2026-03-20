package munchkin.resources.runtime.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import munchkin.resources.runtime.ImageAsyncLoadResult
import munchkin.resources.runtime.RuntimeImageRequestContext
import munchkin.resources.runtime.RuntimeImageSizeLimit
import munchkin.resources.runtime.ResolvableImageSize
import munchkin.resources.runtime.clampTo
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
    context: Context,
    resId: Int,
): Flow<ImageAsyncLoadResult<DrawableAsyncLoadData>> {
    return flow {
        val drawable = AppCompatResources.getDrawable(context, resId)!!
        val result = ImageAsyncLoadResult.Success(drawable.toDrawableAsyncLoadData())
        emit(result)
    }
}

internal fun RequestBuilder<Drawable>.flowDrawableOfSize(
    context: RuntimeImageRequestContext,
    size: ResolvableImageSize,
    sizeLimit: RuntimeImageSizeLimit?,
): Flow<ImageAsyncLoadResult<DrawableAsyncLoadData>> {
    return callbackFlow {
        val target = DrawableFlowTarget(context, this, size, sizeLimit)
        // use add listener
        addListener(target).into(target)
        awaitClose { manager.clear(target) }
    }
}

private class DrawableFlowTarget(
    private val context: RuntimeImageRequestContext,
    private val scope: ProducerScope<ImageAsyncLoadResult<DrawableAsyncLoadData>>,
    private val size: ResolvableImageSize,
    private val sizeLimit: RuntimeImageSizeLimit?,
) : Target<Drawable>, RequestListener<Drawable> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean
    ): Boolean {
        context.logger.error("GlideFlowTarget", e)
        context.listener?.onFailure(model, e ?: GlideException("unknow exception"))
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        context.logger.info("GlideFlowTarget") {
            "onResourceReady first:$isFirstResource source:$dataSource model:$model"
        }
        context.listener?.onSuccess(model)

        return false
    }

    override fun getSize(cb: SizeReadyCallback) {
        scope.launch(Dispatchers.IO) {
            val requested = size.awaitSize()
            val complete = requested.clampTo(sizeLimit)
            if (requested != complete && sizeLimit != null) {
                context.logger.warn(
                    "GlideFlowTarget",
                    "clamp request size: $requested -> $complete by limit=$sizeLimit"
                )
            }
            context.logger.debug("GlideFlowTarget") { "getSize $complete" }
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
        scope.trySend(ImageAsyncLoadResult.Cleared(placeholder?.toDrawableAsyncLoadData()))
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        scope.trySend(ImageAsyncLoadResult.Error(errorDrawable?.toDrawableAsyncLoadData()))
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        scope.trySend(ImageAsyncLoadResult.Success(resource.toDrawableAsyncLoadData()))
    }

    override fun onStart() {}
    override fun onStop() {}
    override fun onDestroy() {}
    override fun onLoadStarted(placeholder: Drawable?) {}
}
