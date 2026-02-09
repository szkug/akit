package cn.szkug.akit.image.coil.support

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.AnimatablePainter
import cn.szkug.akit.image.coil.GifCoilImage
import cn.szkug.akit.image.coil.GifDecodeResult
import coil3.asImage
import coil3.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val DEFAULT_DURATION_MS = 1000

internal actual object GifPlatform {
    actual fun decode(bytes: ByteArray, options: Options): GifDecodeResult {
        val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Unable to decode GIF")

        val srcWidth = movie.width()
        val srcHeight = movie.height()
        if (srcWidth <= 0 || srcHeight <= 0) {
            error("Invalid GIF size: ${srcWidth}x${srcHeight}")
        }

        val (outWidth, outHeight) = computeOutputSize(srcWidth, srcHeight, options)
        val isSampled = outWidth < srcWidth || outHeight < srcHeight
        val durationMs = movie.duration().takeIf { it > 0 } ?: DEFAULT_DURATION_MS
        val repeatCount = gifRepeatCount(parseGifLoopCount(bytes))

        val firstFrame = Bitmap.createBitmap(
            outWidth.coerceAtLeast(1),
            outHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(firstFrame)
        canvas.scale(
            outWidth.toFloat() / srcWidth.toFloat(),
            outHeight.toFloat() / srcHeight.toFloat()
        )
        movie.setTime(0)
        movie.draw(canvas, 0f, 0f)

        val painter = MovieGifPainter(
            movie = movie,
            durationMs = durationMs,
            repeatCount = repeatCount,
            intrinsicWidth = outWidth,
            intrinsicHeight = outHeight,
        )

        val image = GifCoilImage(
            firstFrame = firstFrame.asImage(),
            painter = painter,
            width = outWidth,
            height = outHeight,
            size = outWidth.toLong() * outHeight.toLong() * 4L,
            shareable = false,
        )

        return GifDecodeResult(image = image, isSampled = isSampled)
    }
}

private class MovieGifPainter(
    private val movie: Movie,
    private val durationMs: Int,
    private val repeatCount: Int,
    private val intrinsicWidth: Int,
    private val intrinsicHeight: Int,
) : Painter(), AnimatablePainter, RememberObserver {

    private var frameTimeMs by mutableIntStateOf(0)
    private var drawTick by mutableIntStateOf(0)
    private var animationJob: Job? = null

    override val intrinsicSize: Size = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        Size(intrinsicWidth.toFloat(), intrinsicHeight.toFloat())
    } else {
        Size.Unspecified
    }

    override fun DrawScope.onDraw() {
        drawTick
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return
        val srcWidth = movie.width().toFloat().coerceAtLeast(1f)
        val srcHeight = movie.height().toFloat().coerceAtLeast(1f)
        val scaleX = width / srcWidth
        val scaleY = height / srcHeight
        movie.setTime(frameTimeMs)
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val saveCount = nativeCanvas.save()
            nativeCanvas.scale(scaleX, scaleY)
            movie.draw(nativeCanvas, 0f, 0f)
            nativeCanvas.restoreToCount(saveCount)
        }
    }

    override fun startAnimation(coroutineContext: CoroutineContext) {
        if (animationJob?.isActive == true) return
        val maxLoops = if (repeatCount < 0) Int.MAX_VALUE else repeatCount + 1
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
                    val elapsedMs = ((frameTimeNanos - startNanos) / 1_000_000L).toInt()
                    val loopsDone = elapsedMs / durationMs
                    if (loopsDone >= maxLoops) {
                        stopAnimation()
                        return@withFrameNanos
                    }
                    val timeInLoop = elapsedMs - loopsDone * durationMs
                    frameTimeMs = timeInLoop
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
