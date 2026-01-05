package cn.szkug.akit.compose.image.glide

import android.graphics.drawable.Drawable

internal sealed interface GlideLoadResult<T> {
    @JvmInline
    value class Error<T>(val drawable: Drawable?) : GlideLoadResult<T>

    @JvmInline
    value class Success<T>(val drawable: T) : GlideLoadResult<T>

    @JvmInline
    value class Cleared<T>(val drawable: Drawable?) : GlideLoadResult<T>
}
