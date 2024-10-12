package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.RequestListener

interface PainterRequestListener: RequestListener<Drawable> {
    fun onPainterMemorySize(tag: String, model: Any, size: Int) {}
    fun onLoadDrawable(success: Boolean, drawable: Drawable) {}
}
