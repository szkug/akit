package cn.szkug.akit.image

import androidx.compose.ui.graphics.painter.Painter
import kotlin.jvm.JvmInline


interface AsyncLoadData {
    fun painter(): Painter
}

sealed interface AsyncLoadResult<T: AsyncLoadData> {
    @JvmInline
    value class Error<T: AsyncLoadData>(val data: T?) : AsyncLoadResult<T>

    @JvmInline
    value class Success<T: AsyncLoadData>(val data: T) : AsyncLoadResult<T>

    @JvmInline
    value class Cleared<T: AsyncLoadData>(val data: T?) : AsyncLoadResult<T>
}
