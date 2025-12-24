package com.korilin.akit.publics

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale

object AsyncImageDefaults {
    @Suppress("ConstPropertyName")
    const val DefaultAlpha = androidx.compose.ui.graphics.DefaultAlpha
    val DefaultContentScale = ContentScale.Fit
    val DefaultAlignment = Alignment.Center
    val DefaultColorFilter: ColorFilter? = null
}
