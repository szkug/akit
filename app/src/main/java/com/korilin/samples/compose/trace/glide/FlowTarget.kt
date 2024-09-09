package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
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
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch


internal fun RequestBuilder<Drawable>.flow(
    size: ResolvableGlideSize,
    listener: PainterRequestListener?,
): Flow<GlideLoadResult> {
    return callbackFlow {
        val target = FlowTarget(this, size, listener)
        listener(target).into(target)
        awaitClose { manager.clear(target) }
    }
}

private class FlowTarget(
    private val scope: ProducerScope<GlideLoadResult>,
    private val size: ResolvableGlideSize,
    private val listener: PainterRequestListener?,
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
        GlidePainterLogger.error("FlowTarget", e)
        listener?.onLoadFailed(e, model, target, isFirstResource)
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        GlidePainterLogger.log("FlowTarget") {
            "onResourceReady first:$isFirstResource source:$dataSource Size(${resource.width}, ${resource.height}) model:$model"
        }
        val unBoundMemorySize = when (resource) {
            is ColorDrawable -> 0
            is BitmapDrawable -> resource.bitmap.byteCount
            else -> resource.width * resource.height
        }

        listener?.onPainterMemorySize("FlowTarget", model, unBoundMemorySize)
        listener?.onResourceReady(resource, model, target, dataSource, isFirstResource)
        return false
    }

    override fun getSize(cb: SizeReadyCallback) {
        scope.launch {
            val complete = size.getSize()
            GlidePainterLogger.log("FlowTarget") { "getSize $complete" }
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
        scope.trySend(GlideLoadResult.Error)
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        val painter = resource.toPainter()
        scope.trySend(GlideLoadResult.Success(painter))
    }

    override fun onStart() {}
    override fun onStop() {}
    override fun onDestroy() {}
    override fun onLoadStarted(placeholder: Drawable?) {}
}
