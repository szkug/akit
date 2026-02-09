package cn.szkug.akit.image

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.roundToInt

private fun Float.roundFiniteToInt(sizeOriginal: Int) = if (isFinite()) roundToInt() else sizeOriginal

data class ImageSize(val width: Int, val height: Int)

interface ResolvableImageSize {
    suspend fun awaitSize(): ImageSize
    fun sizeReady(): Boolean
    fun putSize(size: Size)
    fun readySize(): ImageSize?
}

internal class AsyncImageSize(val sizeOriginal: Int) : ResolvableImageSize {

    private val drawSize = MutableSharedFlow<Size>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var readySize: ImageSize? = null

    private var hasEmit = false

    override fun sizeReady(): Boolean {
        return hasEmit && readySize != null
    }

    override fun putSize(size: Size) {
        hasEmit = true
        this.drawSize.tryEmit(size)
    }

    override fun readySize(): ImageSize? {
        return readySize
    }

    override suspend fun awaitSize(): ImageSize {
        return drawSize
            .mapNotNull {
                when {
                    it.isUnspecified -> ImageSize(
                        width = sizeOriginal,
                        height = sizeOriginal
                    )

                    else -> ImageSize(
                        width = it.width.roundFiniteToInt(sizeOriginal),
                        height = it.height.roundFiniteToInt(sizeOriginal)
                    )
                }
            }.first().also {
                readySize = it
            }
    }
}
