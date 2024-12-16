package com.korilin.compose.akit.image.glide

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.roundToInt


private fun Float.roundFiniteToInt() = if (isFinite()) roundToInt() else Target.SIZE_ORIGINAL

internal data class GlideSize(val width: Int, val height: Int)

internal sealed interface ResolvableGlideSize {
    suspend fun awaitSize(): GlideSize
    fun sizeReady(): Boolean
    fun putSize(size: Size)
    fun readySize(): GlideSize?
}

internal data class ImmediateGlideSize(val size: GlideSize) : ResolvableGlideSize {

    constructor(size: Size) : this(
        GlideSize(
            width = size.width.roundFiniteToInt(),
            height = size.height.roundFiniteToInt()
        )
    )

    override suspend fun awaitSize(): GlideSize {
        return size
    }

    override fun readySize(): GlideSize? {
        return size
    }

    override fun sizeReady(): Boolean {
        return true
    }

    override fun putSize(size: Size) {
        // Immediate
    }
}

internal class AsyncGlideSize : ResolvableGlideSize {

    private val drawSize = MutableSharedFlow<Size>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var readySize: GlideSize? = null

    private var hasEmit = false

    override fun sizeReady(): Boolean {
        return hasEmit && readySize != null
    }

    override fun putSize(size: Size) {
        hasEmit = true
        this.drawSize.tryEmit(size)
    }

    override fun readySize(): GlideSize? {
        return readySize
    }

    override suspend fun awaitSize(): GlideSize {
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
            }.first().also {
                readySize = it
            }
    }
}