package munchkin.svga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

internal typealias SvgaPlaybackObserver = () -> Unit

@Stable
class SvgaPlayerState internal constructor(
    iterations: Int,
    autoPlay: Boolean,
) {
    private val playbackObservers = linkedSetOf<SvgaPlaybackObserver>()

    var iterations by mutableIntStateOf(iterations)
        private set

    var isPlaying by mutableStateOf(autoPlay)
        internal set

    var currentFrame by mutableIntStateOf(0)
        internal set

    var completedIterations by mutableIntStateOf(0)
        internal set

    internal var hitRegions: Map<String, Rect> = emptyMap()
    internal var playbackVersion by mutableIntStateOf(0)
        private set

    val progress: Float
        get() = currentFrame.toFloat()

    fun play() {
        isPlaying = true
        notifyPlaybackChanged()
    }

    fun pause() {
        isPlaying = false
        notifyPlaybackChanged()
    }

    fun stop() {
        isPlaying = false
        currentFrame = 0
        completedIterations = 0
        notifyPlaybackChanged()
    }

    fun seekToFrame(frame: Int) {
        currentFrame = frame.coerceAtLeast(0)
        notifyPlaybackChanged()
    }

    fun updateIterations(value: Int) {
        iterations = value
        notifyPlaybackChanged()
    }

    internal fun addPlaybackObserver(observer: SvgaPlaybackObserver) {
        playbackObservers += observer
    }

    internal fun removePlaybackObserver(observer: SvgaPlaybackObserver) {
        playbackObservers -= observer
    }

    private fun notifyPlaybackChanged() {
        playbackVersion += 1
        playbackObservers.forEach { it.invoke() }
    }
}

@Composable
fun rememberSvgaPlayerState(
    iterations: Int = -1,
    autoPlay: Boolean = true,
): SvgaPlayerState = remember(iterations, autoPlay) { SvgaPlayerState(iterations, autoPlay) }
