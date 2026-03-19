package munchkin.svga

import androidx.compose.runtime.Composable

internal interface SvgaAudioController {
    fun resume()
    fun pause()
    fun stop()
    fun onFrame(frameIndex: Int)
}

/**
 * Provides platform objects required to build audio playback controllers outside composition.
 */
internal interface SvgaAudioEnvironment {
    fun createController(movie: SvgaMovie?): SvgaAudioController
}

internal object EmptySvgaAudioController : SvgaAudioController {
    override fun resume() = Unit
    override fun pause() = Unit
    override fun stop() = Unit
    override fun onFrame(frameIndex: Int) = Unit
}

@Composable
internal expect fun rememberSvgaAudioEnvironment(): SvgaAudioEnvironment
