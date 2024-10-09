package com.korilin.samples.compose.trace.glide

import androidx.compose.ui.geometry.Size


/**
 * @property resolveSize the Size used to resolve Glide image loading, which are not used for measurements.
 */
data class GlideExtension(
    val resolveSize: Size? = null
)
