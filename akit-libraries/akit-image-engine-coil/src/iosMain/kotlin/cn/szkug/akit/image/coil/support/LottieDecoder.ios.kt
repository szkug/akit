package cn.szkug.akit.image.coil.support

import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.AnimatablePainter
import cn.szkug.akit.image.coil.LottieCoilImage
import coil3.asImage
import coil3.getExtra
import coil3.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.skottie.Animation
import kotlin.coroutines.CoroutineContext

private const val DEFAULT_DURATION_SEC = 1f

internal actual fun decodeLottie(
    bytes: ByteArray,
    options: Options
): LottieDecodeResult {
    val json = bytes.decodeToString()
    val animation = Animation.makeFromString(json)
    val iterations = options.getExtra(LottieIterationsKey)
    val width = animation.width.toInt()
    val height = animation.height.toInt()

    val painter = LottiePainter(animation, iterations)
    val firstFrameBitmap = Bitmap().apply { allocN32Pixels(width, height) }
    val firstFrameCanvas = Canvas(firstFrameBitmap)
    animation.seekFrameTime(0f)
    val dst = Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat())
    animation.render(firstFrameCanvas, dst)
    val image = LottieCoilImage(
        firstFrame = firstFrameBitmap.asImage(),
        painter = painter,
        width = width,
        height = height,
        size = (width.toLong() * height.toLong() * 4L).coerceAtLeast(0L),
        shareable = false,
    )
    return LottieDecodeResult(image = image, isSampled = false)
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
