package com.korilin.compose.akit.image.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.RequestListener

interface PainterRequestListener: RequestListener<Drawable> {
    fun onPainterMemorySize(tag: String, model: Any, size: Int)
}
