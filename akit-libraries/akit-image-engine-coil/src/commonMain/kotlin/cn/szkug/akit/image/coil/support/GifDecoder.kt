package cn.szkug.akit.image.coil.support

import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import cn.szkug.akit.graph.AnimatablePainter
import cn.szkug.akit.image.coil.GifCoilImage
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.BufferedSource
import okio.use
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

internal data class GifDecodeResult(
    val image: GifCoilImage,
    val isSampled: Boolean,
)

internal expect fun decodeGif(bytes: ByteArray, options: Options): GifDecodeResult

internal class GifDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val mimeType: String?,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val result = decodeGif(bytes, options)
        return DecodeResult(image = result.image, isSampled = result.isSampled)
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isGifSource(result.mimeType, result.source.source().peek())) return null
            return GifDecoder(result.source, options, result.mimeType)
        }
    }
}


private const val DEFAULT_FRAME_DURATION_MS = 100
private const val MIN_FRAME_DURATION_MS = 20

internal class GifPainter(
    private val frames: List<ImageBitmap>,
    private val frameDurationsMs: IntArray,
    private val repeatCount: Int,
) : Painter(), AnimatablePainter, RememberObserver {

    private var frameIndex by mutableIntStateOf(0)
    private var animationJob: Job? = null

    override val intrinsicSize: Size = if (frames.isNotEmpty()) {
        Size(frames[0].width.toFloat(), frames[0].height.toFloat())
    } else {
        Size.Unspecified
    }

    override fun DrawScope.onDraw() {
        if (frames.isEmpty()) return
        val frame = frames[frameIndex.coerceIn(frames.indices)]
        drawImage(
            image = frame,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        )
    }

    override fun startAnimation(coroutineContext: CoroutineContext) {
        if (frames.size <= 1) return
        if (animationJob?.isActive == true) return
        val frameContext = if (coroutineContext[MonotonicFrameClock] == null) {
            @Suppress("DEPRECATION")
            coroutineContext + DefaultMonotonicFrameClock
        } else {
            coroutineContext
        }
        animationJob = CoroutineScope(frameContext).launch {
            val maxLoops = if (repeatCount < 0) Int.MAX_VALUE else repeatCount + 1
            var loops = 0
            var activeFrame = frameIndex.coerceIn(frames.indices)
            var lastFrameTimeNanos = 0L
            while (isActive && loops < maxLoops) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = frameTimeNanos
                    }
                    val durationMs = frameDurationsMs.getOrElse(activeFrame) { DEFAULT_FRAME_DURATION_MS }
                        .coerceAtLeast(MIN_FRAME_DURATION_MS)
                    val durationNanos = durationMs * 1_000_000L
                    if (frameTimeNanos - lastFrameTimeNanos >= durationNanos) {
                        activeFrame++
                        if (activeFrame >= frames.size) {
                            activeFrame = 0
                            loops++
                            if (loops >= maxLoops) return@withFrameNanos
                        }
                        frameIndex = activeFrame
                        lastFrameTimeNanos = frameTimeNanos
                    }
                }
            }
        }
    }

    override fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    override fun onRemembered() {}

    override fun onAbandoned() = stopAnimation()

    override fun onForgotten() = stopAnimation()
}

private const val GIF_HEADER_SIZE = 6L

private fun isGifSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("image/gif", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(GIF_HEADER_SIZE)
    } catch (_: Exception) {
        return false
    }
    return isGifHeader(header)
}

private fun isGifHeader(bytes: ByteArray): Boolean {
    if (bytes.size < GIF_HEADER_SIZE) return false
    return bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()
}

private fun parseGifLoopCount(bytes: ByteArray): Int? {
    var index = 0
    while (index + 2 < bytes.size) {
        if (bytes[index] == 0x21.toByte() && bytes[index + 1] == 0xFF.toByte()) {
            val blockSize = bytes.getOrNull(index + 2)?.toInt() ?: return null
            val appStart = index + 3
            val appEnd = appStart + blockSize
            if (appEnd > bytes.size) return null
            val appId = bytes.copyOfRange(appStart, appEnd)
            if (appId.size >= 11) {
                val appName = appId.copyOfRange(0, 11).decodeToString()
                if (appName == "NETSCAPE2.0" || appName == "ANIMEXTS1.0") {
                    val subBlockStart = appEnd
                    if (subBlockStart + 3 >= bytes.size) return null
                    val subBlockSize = bytes[subBlockStart].toInt() and 0xFF
                    if (subBlockSize >= 3 && bytes[subBlockStart + 1] == 0x01.toByte()) {
                        val lo = bytes[subBlockStart + 2].toInt() and 0xFF
                        val hi = bytes[subBlockStart + 3].toInt() and 0xFF
                        return (hi shl 8) or lo
                    }
                }
            }
        }
        index++
    }
    return null
}

internal fun gifRepeatCount(bytes: ByteArray): Int {
    val loopCount = parseGifLoopCount(bytes) ?: return 0
    return if (loopCount == 0) -1 else loopCount
}
