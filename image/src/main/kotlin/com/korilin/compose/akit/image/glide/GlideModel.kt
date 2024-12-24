package com.korilin.compose.akit.image.glide

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.bumptech.glide.RequestBuilder


sealed interface GlideNodeModel
sealed interface GlidePlaceholderModel

// TODO remove requestBuilder property
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


@Deprecated("""
    Should a network/file image load component not need to support Painter?
    However, it can still be used to support @Compose & @Preview.
""")
@JvmInline
value class PainterModel(val painter: Painter) : GlideNodeModel, GlidePlaceholderModel

@Deprecated("""
    When Drawable already acquired, consider using Image function directly,
    instead of using Glide for load, as internal loading imposes additional UI Performance costs.
""")
@JvmInline
value class DrawableModel(val drawable: Drawable): GlidePlaceholderModel

@JvmInline
value class ResModel(@DrawableRes val id: Int) : GlidePlaceholderModel