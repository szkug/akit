package cn.szkug.akit.publics

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import kotlin.jvm.JvmInline

interface ResourceModel

@JvmInline
@Stable
value class PainterModel(val painter: Painter) : ResourceModel {
    override fun toString(): String {
        return "PainterModel($painter)"
    }

    companion object
}

@JvmInline
@Stable
value class ResIdModel(val resId: Int) : ResourceModel {
    override fun toString(): String {
        return "ResIdModel($resId)"
    }
}
