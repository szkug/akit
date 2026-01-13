package cn.szkug.akit.image

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

@JvmInline
@Stable
value class PathModel(val path: Int) : ResourceModel {
    override fun toString(): String {
        return "PathModel($path)"
    }
}

@JvmInline
@Stable
value class RequestModel(val model: Any?) {
    override fun toString(): String {
        return "AsyncImageRequestModel($model)"
    }
}
