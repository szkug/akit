package munchkin.svga

import androidx.compose.runtime.Composable

internal interface SvgaAudioController {
    fun resume()
    fun pause()
    fun stop()
    fun onFrame(frameIndex: Int)
}

@Composable
internal expect fun rememberSvgaAudioController(movie: SvgaMovie?): SvgaAudioController
