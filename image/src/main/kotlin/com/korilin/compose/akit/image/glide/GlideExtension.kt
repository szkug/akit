package com.korilin.compose.akit.image.glide

import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size

/**
 * TODO refactor internal glide Transformation
 */
interface DrawableTranscoder {
    fun transcode(drawable: Drawable): Drawable
}

/**
 * TODO rename as debug config
 */
data class GlideExtension(
    val transcoder: DrawableTranscoder? = null,
    val ignoreNinePatchPadding: Boolean = false,
    val enableLog: Boolean = false,
) {
    companion object {
        val NORMAL = GlideExtension()
    }
}
