package cn.szkug.akit.compose.image.glide

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import cn.szkug.akit.publics.ResourceModel

@JvmInline
@Stable
value class DrawableModel(val drawable: Drawable) : ResourceModel {

    override fun toString(): String {
        return "DrawableModel($drawable)"
    }
}
