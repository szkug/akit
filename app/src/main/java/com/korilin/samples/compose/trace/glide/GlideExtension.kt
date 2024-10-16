package com.korilin.samples.compose.trace.glide

import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size

interface DrawableTranscoder {
    fun transcode(drawable: Drawable): Drawable
}

/**
 * @property resolveSize the Size used to resolve Glide image loading, which are not used for measurements.
 */
data class GlideExtension(
    val resolveSize: Size? = null,
    val transcoder: DrawableTranscoder? = null,
    val ignoreNinePatchPadding: Boolean = false,
    val enableLog: Boolean = false,
) {
    companion object {
        val NORMAL = GlideExtension()
    }
}
