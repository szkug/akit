package cn.szkug.akit.compose.image.coil

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.roundToInt

internal const val SIZE_ORIGINAL = -1

private fun Float.roundFiniteToInt() = if (isFinite()) roundToInt() else SIZE_ORIGINAL

internal data class CoilSize(val width: Int, val height: Int)

internal sealed interface ResolvableCoilSize {
    suspend fun awaitSize(): CoilSize
    fun sizeReady(): Boolean
    fun putSize(size: Size)
    fun readySize(): CoilSize?
}

internal class AsyncCoilSize : ResolvableCoilSize {

    private val drawSize = MutableSharedFlow<Size>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var readySize: CoilSize? = null

    private var hasEmit = false

    override fun sizeReady(): Boolean {
        return hasEmit && readySize != null
    }

    override fun putSize(size: Size) {
        hasEmit = true
        this.drawSize.tryEmit(size)
    }

    override fun readySize(): CoilSize? {
        return readySize
    }

    override suspend fun awaitSize(): CoilSize {
        return drawSize
            .mapNotNull {
                when {
                    it.isUnspecified -> CoilSize(
                        width = SIZE_ORIGINAL,
                        height = SIZE_ORIGINAL
                    )

                    else -> CoilSize(
                        width = it.width.roundFiniteToInt(),
                        height = it.height.roundFiniteToInt()
                    )
                }
            }.first().also {
                readySize = it
            }
    }
}
