package com.korilin.compose.akit.image.glide

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter


@JvmInline
@Stable
internal value class RequestModel(val model: Any?) {
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

    companion object {
        fun fromId(id: Int?, context: Context): PainterModel? {
            if (id == null) return null
            val drawable = AppCompatResources.getDrawable(context, id) ?: return null
            return PainterModel(drawable.toPainter())
        }
    }
}

