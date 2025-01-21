package com.korilin.akit.compose.image.glide

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter

internal sealed interface GlideLoadResult<T> {
    @JvmInline
    value class Error<T>(val drawable: Drawable?) : GlideLoadResult<T>

    @JvmInline
    value class Success<T>(val drawable: T) : GlideLoadResult<T>

    @JvmInline
    value class Cleared<T>(val drawable: Drawable?) : GlideLoadResult<T>
}