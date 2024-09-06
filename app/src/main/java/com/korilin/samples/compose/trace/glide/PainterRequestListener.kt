package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter
import com.bumptech.glide.request.RequestListener

interface PainterRequestListener: RequestListener<Drawable> {
    @Deprecated("The painter it calls back may not be available")
    fun onPainterReady(painter: Painter)
}
