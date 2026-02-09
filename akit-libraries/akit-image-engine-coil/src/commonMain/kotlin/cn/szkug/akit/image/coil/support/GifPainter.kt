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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

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
