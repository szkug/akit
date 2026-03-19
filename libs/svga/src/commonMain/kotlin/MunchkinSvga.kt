package munchkin.svga

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import munchkin.image.AsyncRequestEngine
import munchkin.image.LocalEngineContextRegister
import munchkin.resources.loader.BinaryRequestEngine
import munchkin.resources.loader.BinarySource
import munchkin.resources.loader.rememberFallbackBinaryPayloadLoader
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
fun MunchkinSvga(
    source: BinarySource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    state: SvgaPlayerState = rememberSvgaPlayerState(),
    dynamicEntity: SvgaDynamicEntity = rememberSvgaDynamicEntity(),
    loadingEngine: AsyncRequestEngine<*>? = null,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    failure: (@Composable BoxScope.(Throwable) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    onLoaded: (SvgaMovie) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val loadState by rememberSvgaComposition(source, loadingEngine)
    val textMeasurer = rememberTextMeasurer()
    val movie = (loadState as? SvgaLoadState.Success)?.value
    val audioController = rememberSvgaAudioController(movie?.movie)

    LaunchedEffect(movie?.movie, state.playbackVersion, state.isPlaying) {
        val prepared = movie ?: return@LaunchedEffect
        val frames = prepared.movie.frames.coerceAtLeast(1)
        val frameDelayMs = (1000f / prepared.movie.fps.coerceAtLeast(1)).roundToLong().coerceAtLeast(16L)
        if (!state.isPlaying) {
            audioController.pause()
            return@LaunchedEffect
        }
        audioController.resume()
        audioController.onFrame(state.currentFrame.coerceIn(0, frames - 1))
        while (state.isPlaying) {
            delay(frameDelayMs)
            val next = state.currentFrame + 1
            if (next >= frames) {
                val targetIterations = state.iterations
                if (targetIterations >= 0 && state.completedIterations + 1 >= targetIterations) {
                    state.currentFrame = frames - 1
                    state.isPlaying = false
                    audioController.stop()
                    break
                }
                state.completedIterations += 1
                state.currentFrame = 0
                audioController.onFrame(0)
            } else {
                state.currentFrame = next
                audioController.onFrame(next)
            }
        }
    }

    LaunchedEffect(movie?.movie) {
        movie?.movie?.let(onLoaded)
    }

    LaunchedEffect((loadState as? SvgaLoadState.Error)?.error) {
        (loadState as? SvgaLoadState.Error)?.error?.let(onError)
    }

    Box(modifier = modifier) {
        when (val result = loadState) {
            SvgaLoadState.Loading -> placeholder?.invoke(this)
            is SvgaLoadState.Error -> failure?.invoke(this, result.error)
            is SvgaLoadState.Success -> {
                val painter = rememberSvgaPainter(
                    prepared = result.value,
                    state = state,
                    dynamicEntity = dynamicEntity,
                    contentScale = contentScale,
                    alignment = alignment,
                    textMeasurer = textMeasurer,
                )
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dynamicEntity, state) {
                            detectTapGestures { offset ->
                                dynamicEntity.dispatchClick(offset, state.hitRegions)
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun rememberSvgaComposition(
    source: BinarySource,
    loadingEngine: AsyncRequestEngine<*>?,
): State<SvgaLoadState> {
    val fallbackLoader = rememberFallbackBinaryPayloadLoader()
    val engineContext = loadingEngine?.let { LocalEngineContextRegister.resolve(it) }
    return produceState<SvgaLoadState>(initialValue = SvgaLoadState.Loading, source, loadingEngine, engineContext, fallbackLoader) {
        value = runCatching {
            val payload = when {
                loadingEngine is BinaryRequestEngine && engineContext != null ->
                    loadingEngine.requestBinary(engineContext, source).payload

                else -> fallbackLoader(source)
            }
            prepareSvgaMovie(SvgaDecoder.decode(payload.bytes))
        }.fold(
            onSuccess = { SvgaLoadState.Success(it) },
            onFailure = { SvgaLoadState.Error(it) },
        )
    }
}

private sealed interface SvgaLoadState {
    data object Loading : SvgaLoadState
    data class Success(val value: PreparedSvgaMovie) : SvgaLoadState
    data class Error(val error: Throwable) : SvgaLoadState
}

@Composable
private fun rememberSvgaPainter(
    prepared: PreparedSvgaMovie,
    state: SvgaPlayerState,
    dynamicEntity: SvgaDynamicEntity,
    contentScale: ContentScale,
    alignment: Alignment,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): Painter {
    return remember(prepared, state, dynamicEntity, contentScale, alignment, textMeasurer) {
        SvgaPainter(prepared, state, dynamicEntity, contentScale, alignment, textMeasurer)
    }
}
