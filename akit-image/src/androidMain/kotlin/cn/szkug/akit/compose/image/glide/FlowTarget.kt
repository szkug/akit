package cn.szkug.akit.compose.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
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

internal fun flowOfId(
    context: Context,
    resId: Int,
): Flow<GlideLoadResult<Drawable>> {
    return flow {
        GlideDefaults.logger.error("FlowTarget", "flowOfId $resId")
        val drawable = AppCompatResources.getDrawable(context, resId)!!
        val result = GlideLoadResult.Success(drawable)
        emit(result)
    }
}

internal fun <T : Any> RequestBuilder<T>.flowOfSize(
    size: ResolvableGlideSize,
): Flow<GlideLoadResult<T>> {
    return callbackFlow {
        val target = FlowTarget(this, size)
        // use add listener
        addListener(target).into(target)
        awaitClose { manager.clear(target) }
    }
}

private class FlowTarget<T : Any>(
    private val scope: ProducerScope<GlideLoadResult<T>>,
    private val size: ResolvableGlideSize,
) : Target<T>, RequestListener<T> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<T>,
        isFirstResource: Boolean
    ): Boolean {
        GlideDefaults.logger.error("FlowTarget", e)
        return false
    }

    override fun onResourceReady(
        resource: T,
        model: Any,
        target: Target<T>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        GlideDefaults.logger.info("FlowTarget") {
            "onResourceReady first:$isFirstResource source:$dataSource model:$model"
        }

        return false
    }

    override fun getSize(cb: SizeReadyCallback) {
        scope.launch(Dispatchers.IO) {
            val complete = size.awaitSize()
            GlideDefaults.logger.info("FlowTarget") { "getSize $complete" }
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
        scope.trySend(GlideLoadResult.Cleared(placeholder))
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        scope.trySend(GlideLoadResult.Error(errorDrawable))
    }

    override fun onResourceReady(resource: T, transition: Transition<in T>?) {
        scope.trySend(GlideLoadResult.Success(resource))
    }

    override fun onStart() {}
    override fun onStop() {}
    override fun onDestroy() {}
    override fun onLoadStarted(placeholder: Drawable?) {}
}
