package com.korilin.akit.compose.image.glide

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import com.korilin.akit.publics.ResourceModel

@JvmInline
@Stable
value class RequestModel(val model: Any?) {
    override fun toString(): String {
        return "GlideRequestModel($model)"
    }
}

@JvmInline
@Stable
value class DrawableModel(val drawable: Drawable) : ResourceModel {

    override fun toString(): String {
        return "DrawableModel($drawable)"
    }
}
