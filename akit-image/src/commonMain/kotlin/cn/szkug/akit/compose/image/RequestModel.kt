package cn.szkug.akit.compose.image

import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

@JvmInline
@Stable
value class RequestModel(val model: Any?) {
    override fun toString(): String {
        return "AsyncImageRequestModel($model)"
    }
}
