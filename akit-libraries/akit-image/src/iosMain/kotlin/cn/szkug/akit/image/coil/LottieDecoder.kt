package cn.szkug.akit.image.coil

import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.AnimatablePainter
import coil3.Extras
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import okio.use
import org.jetbrains.skia.Rect
import org.jetbrains.skia.skottie.Animation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal val LottieDecodeEnabled = Extras.Key(false)
internal val LottieIterationsKey = Extras.Key(-1)

private const val DEFAULT_DURATION_SEC = 1f

internal class LottieCoilImage(
    private val animation: Animation,
    private val iterations: Int,
) : Image {
    override val width: Int = animation.width.toInt()
    override val height: Int = animation.height.toInt()
    override val size: Long = (width.toLong() * height.toLong() * 4L).coerceAtLeast(0L)
    override val shareable: Boolean = false

    override fun draw(canvas: coil3.Canvas) {
        animation.seekFrameTime(0f)
        val dst = Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat())
        animation.render(canvas, dst)
    }

    fun toPainter(): Painter = LottiePainter(animation, iterations)
}

internal class LottieDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        if (bytes.isEmpty()) {
            error("Lottie source is empty.")
        }
        val json = bytes.decodeToString()
        val animation = Animation.makeFromString(json)
        val iterations = options.getExtra(LottieIterationsKey)
        val image = LottieCoilImage(animation, iterations)
        return DecodeResult(image = image, isSampled = false)
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!options.getExtra(LottieDecodeEnabled)) return null
            return LottieDecoder(result.source, options)
        }
    }
}

private class LottiePainter(
    private val animation: Animation,
    private val iterations: Int,
) : Painter(), AnimatablePainter, RememberObserver {

    private var frameTimeSeconds by mutableFloatStateOf(0f)
    private var drawTick by mutableIntStateOf(0)
    private var animationJob: Job? = null

    init {
        animation.seekFrameTime(0f)
    }

    override val intrinsicSize: Size = Size(animation.width, animation.height)

    override fun DrawScope.onDraw() {
        drawTick
        val dst = Rect.makeXYWH(0f, 0f, size.width, size.height)
        animation.seekFrameTime(frameTimeSeconds)
        drawIntoCanvas { canvas ->
            animation.render(canvas.nativeCanvas, dst)
        }
    }

    override fun startAnimation(coroutineContext: CoroutineContext) {
        if (animationJob?.isActive == true) return
        val maxLoops = if (iterations < 0) Int.MAX_VALUE else iterations + 1
        val duration = animation.duration.takeIf { it > 0f } ?: DEFAULT_DURATION_SEC
        val frameContext = if (coroutineContext[MonotonicFrameClock] == null) {
            @Suppress("DEPRECATION")
            coroutineContext + DefaultMonotonicFrameClock
        } else {
            coroutineContext
        }
        animationJob = CoroutineScope(frameContext).launch {
            var startNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (startNanos == 0L) startNanos = frameTimeNanos
                    val elapsedSec = (frameTimeNanos - startNanos) / 1_000_000_000.0
                    val loopsDone = (elapsedSec / duration).toInt()
                    if (loopsDone >= maxLoops) {
                        stopAnimation()
                        return@withFrameNanos
                    }
                    val timeInLoop = elapsedSec - loopsDone * duration
                    frameTimeSeconds = timeInLoop.toFloat()
                    drawTick++
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
