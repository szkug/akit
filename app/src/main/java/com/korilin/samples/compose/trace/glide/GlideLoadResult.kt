package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter

internal sealed interface GlideLoadResult {
    @JvmInline
    value class Error(val drawable: Drawable?) : GlideLoadResult

    @JvmInline
    value class Success(val drawable: Drawable) : GlideLoadResult

    object Cleared : GlideLoadResult
}