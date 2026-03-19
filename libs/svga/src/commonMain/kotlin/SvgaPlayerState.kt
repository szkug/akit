package munchkin.svga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

@Stable
class SvgaPlayerState internal constructor(
    iterations: Int,
    autoPlay: Boolean,
) {
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

    val progress: Float
        get() = currentFrame.toFloat()

    fun play() {
        isPlaying = true
        playbackVersion += 1
    }

    fun pause() {
        isPlaying = false
        playbackVersion += 1
    }

    fun stop() {
        isPlaying = false
        currentFrame = 0
        completedIterations = 0
        playbackVersion += 1
    }

    fun seekToFrame(frame: Int) {
        currentFrame = frame.coerceAtLeast(0)
        playbackVersion += 1
    }

    fun updateIterations(value: Int) {
        iterations = value
        playbackVersion += 1
    }
}

@Composable
fun rememberSvgaPlayerState(
    iterations: Int = -1,
    autoPlay: Boolean = true,
): SvgaPlayerState = remember(iterations, autoPlay) { SvgaPlayerState(iterations, autoPlay) }
