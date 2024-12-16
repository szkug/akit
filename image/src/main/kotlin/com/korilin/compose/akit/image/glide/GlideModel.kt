package com.korilin.compose.akit.image.glide

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.bumptech.glide.RequestBuilder


sealed interface GlideNodeModel
sealed interface GlidePlaceholderModel

@Stable
internal class GlideRequestModel(
    val model: Any?,
    val requestBuilder: () -> RequestBuilder<Drawable>,
) : GlideNodeModel {

    override fun equals(other: Any?): Boolean {
        if (other !is GlideRequestModel) return false
        return other.model == model
    }

    override fun toString(): String {
        return "GlideRequestModel($model)"
    }

    override fun hashCode(): Int {
        return model?.hashCode() ?: 0
    }
}

@JvmInline
value class PainterModel(val painter: Painter) : GlideNodeModel, GlidePlaceholderModel

@JvmInline
value class DrawableModel(val drawable: Drawable): GlidePlaceholderModel

@JvmInline
value class ResModel(@DrawableRes val id: Int) : GlidePlaceholderModel