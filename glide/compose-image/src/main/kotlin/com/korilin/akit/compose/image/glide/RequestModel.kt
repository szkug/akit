package com.korilin.akit.compose.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.DrawableResource


sealed interface ResourceModel

@JvmInline
@Stable
value class RequestModel(val model: Any?) {
    override fun toString(): String {
        return "GlideRequestModel($model)"
    }
}

@JvmInline
@Stable
value class DrawableModel(val drawable: Drawable): ResourceModel {

    override fun toString(): String {
        return "DrawableModel($drawable)"
    }
}

@JvmInline
@Stable
value class ResIdModel(val resId: Int): ResourceModel {

    override fun toString(): String {
        return "ResModel($resId)"
    }
}

@JvmInline
@Stable
value class PainterModel(val painter: Painter): ResourceModel {

    override fun toString(): String {
        return "PainterModel($painter)"
    }

    companion object {
        fun fromId(id: Int?, context: Context): PainterModel? {
            if (id == null) return null
            val drawable = AppCompatResources.getDrawable(context, id) ?: return null
            return PainterModel(drawable.toPainter())
        }

        @Composable
        fun fromId(id: Int?): PainterModel? {
            if (id == null) return null
            val context = LocalContext.current
            val drawable = AppCompatResources.getDrawable(context, id) ?: return null
            return PainterModel(drawable.toPainter())
        }
    }
}

