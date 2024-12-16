package com.korilin.compose.akit.image.glide

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import kotlinx.coroutines.launch


internal fun RequestBuilder<Drawable>.flow(
    size: ResolvableGlideSize,
): Flow<GlideLoadResult> {
    return callbackFlow {
        val target = FlowTarget(this, size)
        // use add listener
        addListener(target).into(target)
        awaitClose { manager.clear(target) }
    }
}

private class FlowTarget(
    private val scope: ProducerScope<GlideLoadResult>,
    private val size: ResolvableGlideSize,
) : Target<Drawable>, RequestListener<Drawable> {

    private val Drawable.width: Int
        get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

    private val Drawable.height: Int
        get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean
    ): Boolean {
        GlideDefaults.logger.error("FlowTarget", e)
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        GlideDefaults.logger.info("FlowTarget") {
            "onResourceReady first:$isFirstResource source:$dataSource Size(${resource.width}, ${resource.height}) model:$model"
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
        scope.trySend(GlideLoadResult.Cleared)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        scope.trySend(GlideLoadResult.Error(errorDrawable))
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        scope.trySend(GlideLoadResult.Success(resource))
    }

    override fun onStart() {}
    override fun onStop() {}
    override fun onDestroy() {}
    override fun onLoadStarted(placeholder: Drawable?) {}
}
