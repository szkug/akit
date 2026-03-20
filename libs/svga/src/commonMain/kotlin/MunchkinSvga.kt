package munchkin.svga

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.rememberTextMeasurer
import munchkin.resources.runtime.BinarySource
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.LocalRuntimeEngineContextRegister
import munchkin.resources.runtime.RuntimeSvgaRequestEngine

/**
 * Renders one SVGA animation through a dedicated modifier node.
 *
 * The draw path now mirrors `MunchkinAsyncImage`:
 * - source loading and decode happen inside the node instead of `produceState`
 * - playback timing lives in the node instead of `LaunchedEffect`
 * - the common success path draws directly through the node without `Image` or async painters
 *
 * Placeholder and failure slots are still supported. They are only composed when the coarse load
 * state changes, so frame progression stays outside composition.
 *
 * Callers must provide a loader engine explicitly. Unlike the internal decoder, source acquisition
 * is never resolved through an implicit fallback path.
 */
@Composable
fun <C : RuntimeEngineContext> MunchkinSvga(
    source: BinarySource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    state: SvgaPlayerState = rememberSvgaPlayerState(),
    dynamicEntity: SvgaDynamicEntity = SvgaDynamicEntity.EMPTY,
    loaderEngine: RuntimeSvgaRequestEngine<C>,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    failure: (@Composable BoxScope.(Throwable) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    onLoaded: (SvgaMovie) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val engineContext = LocalRuntimeEngineContextRegister.resolve(loaderEngine)
    val audioEnvironment = rememberSvgaAudioEnvironment()
    val textMeasurer = rememberTextMeasurer()
    val slotState = remember(source, loaderEngine) { SvgaSlotState() }

    Box(modifier = modifier) {
        Layout(
            modifier = Modifier
                .pointerInput(dynamicEntity, state) {
                    detectTapGestures { offset ->
                        dynamicEntity.dispatchClick(offset, state.hitRegions)
                    }
                }
                .svgaNode(
                    source = source,
                    state = state,
                    dynamicEntity = dynamicEntity,
                    loaderEngine = loaderEngine,
                    engineContext = engineContext,
                    audioEnvironment = audioEnvironment,
                    slotState = slotState,
                    contentDescription = contentDescription,
                    alignment = alignment,
                    contentScale = contentScale,
                    textMeasurer = textMeasurer,
                    onLoaded = onLoaded,
                    onError = onError,
                ),
            measurePolicy = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            },
        )
        when (val overlay = slotState.overlay) {
            is SvgaOverlay.Error -> failure?.invoke(this, overlay.throwable)
            SvgaOverlay.Loading -> placeholder?.invoke(this)
            SvgaOverlay.Success -> Unit
        }
    }
}
