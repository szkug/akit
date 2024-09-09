package com.korilin.samples.compose.trace.glide

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.roundToInt


internal data class GlideSize(val width: Int, val height: Int)


internal sealed interface ResolvableGlideSize {
    suspend fun getSize(): GlideSize
}

internal data class ImmediateGlideSize(val size: GlideSize) : ResolvableGlideSize {
    override suspend fun getSize(): GlideSize {
        return size
    }
}

internal class AsyncGlideSize : ResolvableGlideSize {

    private val drawSize = MutableSharedFlow<Size>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    var hasEmit = false
        private set

    suspend fun emit(size: Size) {
        hasEmit = true
        this.drawSize.emit(size)
    }

    fun tryEmit(size: Size) {
        hasEmit = true
        this.drawSize.tryEmit(size)
    }

    private fun Float.roundFiniteToInt() = if (isFinite()) roundToInt() else Target.SIZE_ORIGINAL

    override suspend fun getSize(): GlideSize {
        return drawSize
            .mapNotNull {
                when {
                    it.isUnspecified -> GlideSize(
                        width = Target.SIZE_ORIGINAL,
                        height = Target.SIZE_ORIGINAL
                    )

                    else -> GlideSize(
                        width = it.width.roundFiniteToInt(),
                        height = it.height.roundFiniteToInt()
                    )
                }
            }.first()
    }
}