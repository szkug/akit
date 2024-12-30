package com.korilin.compose.akit.image.glide

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

sealed interface RequestModel

@JvmInline
@Stable
internal value class GlideRequestModel(val model: Any?) : RequestModel {
    override fun toString(): String {
        return "GlideRequestModel($model)"
    }
}

@JvmInline
@Stable
internal value class ResModel(val resId: Int) {

    override fun toString(): String {
        return "ResModel($resId)"
    }
}

@JvmInline
@Stable
internal value class PainterModel(val painter: Painter) {

    override fun toString(): String {
        return "PainterModel($painter)"
    }
}

