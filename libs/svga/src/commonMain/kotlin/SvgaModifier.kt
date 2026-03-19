package munchkin.svga

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.trace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.SvgaAsyncRequestEngine
import munchkin.resources.loader.BinarySource
import kotlin.math.max
import kotlin.math.roundToInt

private const val TRACE_SECTION_NAME = "SvgaModifier"

/**
 * Compose-visible state bridge for optional placeholder and failure content.
 *
 * The node owns loading and decode work. Composition only observes this coarse state so the common
 * playback path does not recompose for every frame.
 */
@Stable
internal class SvgaSlotState {
    var overlay: SvgaOverlay by mutableStateOf(SvgaOverlay.Loading)
}

internal sealed interface SvgaOverlay {
    data object Loading : SvgaOverlay
    data object Success : SvgaOverlay
    data class Error(val throwable: Throwable) : SvgaOverlay
}

internal fun Modifier.svgaNode(
    source: BinarySource,
    state: SvgaPlayerState,
    dynamicEntity: SvgaDynamicEntity,
    loaderEngine: SvgaAsyncRequestEngine?,
    engineContext: EngineContext?,
    fallbackLoader: suspend (BinarySource) -> munchkin.resources.loader.BinaryPayload,
    audioEnvironment: SvgaAudioEnvironment,
    slotState: SvgaSlotState,
    contentDescription: String?,
    alignment: Alignment,
    contentScale: ContentScale,
    textMeasurer: TextMeasurer,
    onLoaded: (SvgaMovie) -> Unit,
    onError: (Throwable) -> Unit,
): Modifier = clipToBounds()
    .semantics {
        if (contentDescription != null) {
            this@semantics.contentDescription = contentDescription
        }
        role = Role.Image
    } then SvgaElement(
    source = source,
    state = state,
    dynamicEntity = dynamicEntity,
    loaderEngine = loaderEngine,
    engineContext = engineContext,
    fallbackLoader = fallbackLoader,
    audioEnvironment = audioEnvironment,
    slotState = slotState,
    alignment = alignment,
    contentScale = contentScale,
    textMeasurer = textMeasurer,
    onLoaded = onLoaded,
    onError = onError,
)

private data class SvgaElement(
    val source: BinarySource,
    val state: SvgaPlayerState,
    val dynamicEntity: SvgaDynamicEntity,
    val loaderEngine: SvgaAsyncRequestEngine?,
    val engineContext: EngineContext?,
    val fallbackLoader: suspend (BinarySource) -> munchkin.resources.loader.BinaryPayload,
    val audioEnvironment: SvgaAudioEnvironment,
    val slotState: SvgaSlotState,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val textMeasurer: TextMeasurer,
    val onLoaded: (SvgaMovie) -> Unit,
    val onError: (Throwable) -> Unit,
) : ModifierNodeElement<SvgaNode>() {

    override fun create(): SvgaNode {
        return SvgaNode(
            source = source,
            state = state,
            dynamicEntity = dynamicEntity,
            loaderEngine = loaderEngine,
            engineContext = engineContext,
            fallbackLoader = fallbackLoader,
            audioEnvironment = audioEnvironment,
            slotState = slotState,
            alignment = alignment,
            contentScale = contentScale,
            textMeasurer = textMeasurer,
            onLoaded = onLoaded,
            onError = onError,
        )
    }

    override fun update(node: SvgaNode) {
        node.update(
            source = source,
            state = state,
            dynamicEntity = dynamicEntity,
            loaderEngine = loaderEngine,
            engineContext = engineContext,
            fallbackLoader = fallbackLoader,
            audioEnvironment = audioEnvironment,
            slotState = slotState,
            alignment = alignment,
            contentScale = contentScale,
            textMeasurer = textMeasurer,
            onLoaded = onLoaded,
            onError = onError,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "Svga"
        properties["source"] = source
        properties["contentScale"] = contentScale
        properties["alignment"] = alignment
        properties["loaderEngine"] = loaderEngine
    }
}

/**
 * Node that owns SVGA loading, decode, playback timing, and draw invalidation.
 */
internal class SvgaNode(
    private var source: BinarySource,
    private var state: SvgaPlayerState,
    private var dynamicEntity: SvgaDynamicEntity,
    private var loaderEngine: SvgaAsyncRequestEngine?,
    private var engineContext: EngineContext?,
    private var fallbackLoader: suspend (BinarySource) -> munchkin.resources.loader.BinaryPayload,
    private var audioEnvironment: SvgaAudioEnvironment,
    private var slotState: SvgaSlotState,
    private var alignment: Alignment,
    private var contentScale: ContentScale,
    private var textMeasurer: TextMeasurer,
    private var onLoaded: (SvgaMovie) -> Unit,
    private var onError: (Throwable) -> Unit,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {

    private var renderer: SvgaRenderer? = null
    private var preparedMovie: PreparedSvgaMovie? = null
    private var audioController: SvgaAudioController = EmptySvgaAudioController
    private var loadJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    private var playbackJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
    private var hasFixedSize = false
    private val playbackObserver: SvgaPlaybackObserver = {
        syncPlaybackFromControls()
        invalidateDraw()
    }

    override fun onAttach() {
        super.onAttach()
        SvgaLogger.debug("SvgaNode") { "attach source=${source.logLabel()}" }
        state.addPlaybackObserver(playbackObserver)
        startLoad()
    }

    override fun onDetach() {
        super.onDetach()
        SvgaLogger.debug("SvgaNode") { "detach source=${source.logLabel()}" }
        state.removePlaybackObserver(playbackObserver)
        releaseRuntime(resetOverlay = false)
    }

    override fun onReset() {
        super.onReset()
        SvgaLogger.debug("SvgaNode") { "reset source=${source.logLabel()}" }
        releaseRuntime(resetOverlay = true)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult = trace("$TRACE_SECTION_NAME.measure") {
        val modified = modifyConstraints(constraints)
        hasFixedSize = modified.hasFixedSize()
        val placeable = measurable.measure(modified)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        val layoutWidth = measurable.minIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        val layoutWidth = measurable.maxIntrinsicWidth(height)
        return modifyIntrinsicWidth(height, layoutWidth)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        val layoutHeight = measurable.minIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        val layoutHeight = measurable.maxIntrinsicHeight(width)
        return modifyIntrinsicHeight(width, layoutHeight)
    }

    override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() = trace("$TRACE_SECTION_NAME.draw") {
        val activeRenderer = renderer
        if (activeRenderer == null) {
            state.hitRegions = emptyMap()
            drawContent()
            return@trace
        }
        state.hitRegions = with(activeRenderer) {
            draw(state.currentFrame)
        }
        drawContent()
    }

    fun update(
        source: BinarySource,
        state: SvgaPlayerState,
        dynamicEntity: SvgaDynamicEntity,
        loaderEngine: SvgaAsyncRequestEngine?,
        engineContext: EngineContext?,
        fallbackLoader: suspend (BinarySource) -> munchkin.resources.loader.BinaryPayload,
        audioEnvironment: SvgaAudioEnvironment,
        slotState: SvgaSlotState,
        alignment: Alignment,
        contentScale: ContentScale,
        textMeasurer: TextMeasurer,
        onLoaded: (SvgaMovie) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        var needsReload = false
        var needsDraw = false
        var needsMeasurement = false
        var needsPlaybackSync = false

        if (state !== this.state) {
            this.state.removePlaybackObserver(playbackObserver)
            this.state = state
            this.state.addPlaybackObserver(playbackObserver)
            needsDraw = true
            needsPlaybackSync = true
        }
        if (source != this.source) {
            this.source = source
            needsReload = true
        }
        if (dynamicEntity !== this.dynamicEntity) {
            this.dynamicEntity = dynamicEntity
            needsDraw = true
        }
        if (loaderEngine != this.loaderEngine) {
            this.loaderEngine = loaderEngine
            needsReload = true
        }
        if (engineContext != this.engineContext) {
            this.engineContext = engineContext
            needsReload = true
        }
        if (fallbackLoader !== this.fallbackLoader) {
            this.fallbackLoader = fallbackLoader
            needsReload = true
        }
        if (audioEnvironment !== this.audioEnvironment) {
            this.audioEnvironment = audioEnvironment
            replaceAudioController(preparedMovie?.movie)
            needsPlaybackSync = true
        }
        if (slotState !== this.slotState) {
            this.slotState = slotState
            this.slotState.overlay = currentOverlay()
        }
        if (alignment != this.alignment) {
            this.alignment = alignment
            needsDraw = true
        }
        if (contentScale != this.contentScale) {
            this.contentScale = contentScale
            needsDraw = true
            needsMeasurement = true
        }
        if (textMeasurer != this.textMeasurer) {
            this.textMeasurer = textMeasurer
            needsDraw = true
        }
        this.onLoaded = onLoaded
        this.onError = onError

        if (needsReload) {
            SvgaLogger.debug("SvgaNode") { "update requires reload source=${source.logLabel()}" }
            startLoad(forceRestart = true)
            return
        }

        if (needsDraw) {
            rebuildRenderer()
            invalidateDraw()
        }
        if (needsPlaybackSync) {
            syncPlaybackFromControls()
        }
        if (needsMeasurement) {
            invalidateMeasurement()
        }
    }

    private fun modifyIntrinsicWidth(height: Int, layoutWidth: Int): Int {
        return if (rendererIntrinsicSizeSpecified) {
            val constraints = modifyConstraints(Constraints(maxHeight = height))
            max(constraints.minWidth, layoutWidth)
        } else {
            layoutWidth
        }
    }

    private fun modifyIntrinsicHeight(width: Int, layoutHeight: Int): Int {
        return if (rendererIntrinsicSizeSpecified) {
            val constraints = modifyConstraints(Constraints(maxWidth = width))
            max(constraints.minHeight, layoutHeight)
        } else {
            layoutHeight
        }
    }

    private val rendererIntrinsicSizeSpecified: Boolean
        get() = renderer?.intrinsicSize?.isSpecified == true

    private fun modifyConstraints(constraints: Constraints): Constraints = trace("$TRACE_SECTION_NAME.modifyConstraints") {
        if (constraints.hasFixedWidth && constraints.hasFixedHeight) return constraints
        val intrinsicSize = renderer?.intrinsicSize ?: return constraints
        val hasBoundedDimensions = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        if (!intrinsicSize.isSpecified && hasBoundedDimensions) {
            return constraints
        }

        val intrinsicWidth = if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
            intrinsicSize.width.roundToInt()
        } else {
            constraints.minWidth
        }
        val intrinsicHeight = if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
            intrinsicSize.height.roundToInt()
        } else {
            constraints.minHeight
        }
        val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
        val constrainedHeight = constraints.constrainHeight(intrinsicHeight)
        val scaledSize = calculateScaledSize(Size(constrainedWidth.toFloat(), constrainedHeight.toFloat()))
        val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
        val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
        constraints.copy(minWidth = minWidth, minHeight = minHeight)
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        val intrinsicSize = renderer?.intrinsicSize ?: return dstSize
        return if (intrinsicSize.isSpecified) {
            val srcWidth = if (!intrinsicSize.hasSpecifiedAndFiniteWidth()) dstSize.width else intrinsicSize.width
            val srcHeight = if (!intrinsicSize.hasSpecifiedAndFiniteHeight()) dstSize.height else intrinsicSize.height
            val srcSize = Size(srcWidth, srcHeight)
            if (dstSize.width != 0f && dstSize.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
            } else {
                Size.Zero
            }
        } else {
            dstSize
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun startLoad(forceRestart: Boolean = false) = sideEffect {
        if (forceRestart) {
            releaseRuntime(resetOverlay = true)
        }
        if (loadJob != null) return@sideEffect
        val activeSource = source
        slotState.overlay = SvgaOverlay.Loading
        SvgaLogger.info("SvgaLoad") {
            "start source=${activeSource.logLabel()} engine=${loaderEngine?.let { it::class.simpleName } ?: "fallback"}"
        }
        loadJob = coroutineScope.launch {
            val result = try {
                val payload = when {
                    loaderEngine != null && engineContext != null -> loaderEngine!!.requestSvga(engineContext!!, source).payload
                    else -> fallbackLoader(source)
                }
                Result.success(prepareSvgaMovie(SvgaDecoder.decode(payload.bytes)))
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    loadJob = null
                    SvgaLogger.debug("SvgaLoad") { "cancelled source=${activeSource.logLabel()}" }
                    throw throwable
                }
                Result.failure(throwable)
            }
            loadJob = null
            result.fold(
                onSuccess = { prepared ->
                    preparedMovie = prepared
                    replaceAudioController(prepared.movie)
                    rebuildRenderer()
                    slotState.overlay = SvgaOverlay.Success
                    SvgaLogger.info("SvgaLoad") {
                        "success source=${activeSource.logLabel()} frames=${prepared.movie.frames} fps=${prepared.movie.fps} sprites=${prepared.movie.sprites.size} bitmaps=${prepared.bitmaps.size} audios=${prepared.movie.audioAssets.size}"
                    }
                    state.hitRegions = emptyMap()
                    syncPlaybackFromControls()
                    onLoaded(prepared.movie)
                    if (!hasFixedSize) invalidateMeasurement()
                    invalidateDraw()
                },
                onFailure = { throwable ->
                    preparedMovie = null
                    renderer = null
                    replaceAudioController(null)
                    slotState.overlay = SvgaOverlay.Error(throwable)
                    SvgaLogger.error("SvgaLoad", throwable) {
                        "failed source=${activeSource.logLabel()} message=${throwable.message.orEmpty()}"
                    }
                    state.hitRegions = emptyMap()
                    onError(throwable)
                    if (!hasFixedSize) invalidateMeasurement()
                    invalidateDraw()
                },
            )
        }
    }

    private fun rebuildRenderer() {
        renderer = preparedMovie?.let {
            SvgaRenderer(
                prepared = it,
                dynamicEntity = dynamicEntity,
                contentScale = contentScale,
                alignment = alignment,
                textMeasurer = textMeasurer,
            )
        }
    }

    private fun currentOverlay(): SvgaOverlay {
        return when {
            renderer != null -> SvgaOverlay.Success
            loadJob != null -> SvgaOverlay.Loading
            else -> slotState.overlay
        }
    }

    private fun replaceAudioController(movie: SvgaMovie?) {
        audioController.stop()
        audioController = movie?.let(audioEnvironment::createController) ?: EmptySvgaAudioController
    }

    private fun syncPlaybackFromControls() {
        val prepared = preparedMovie ?: run {
            playbackJob = null
            return
        }
        playbackJob = null

        val frameCount = prepared.movie.frames.coerceAtLeast(1)
        val clampedFrame = state.currentFrame.coerceIn(0, frameCount - 1)
        if (clampedFrame != state.currentFrame) {
            state.currentFrame = clampedFrame
        }

        if (!state.isPlaying) {
            SvgaLogger.debug("SvgaPlayback") {
                "pause source=${source.logLabel()} frame=${state.currentFrame} completedIterations=${state.completedIterations}"
            }
            audioController.pause()
            invalidateDraw()
            return
        }

        SvgaLogger.debug("SvgaPlayback") {
            "start source=${source.logLabel()} frame=${state.currentFrame} iterations=${state.iterations}"
        }
        playbackJob = coroutineScope.launch {
            val frameDelayMs = (1000f / prepared.movie.fps.coerceAtLeast(1)).roundToInt().toLong().coerceAtLeast(16L)
            audioController.resume()
            audioController.onFrame(state.currentFrame)
            invalidateDraw()
            while (isActive && state.isPlaying) {
                delay(frameDelayMs)
                val nextFrame = state.currentFrame + 1
                if (nextFrame >= frameCount) {
                    val targetIterations = state.iterations
                    if (targetIterations >= 0 && state.completedIterations + 1 >= targetIterations) {
                        state.currentFrame = frameCount - 1
                        state.isPlaying = false
                        audioController.stop()
                        SvgaLogger.info("SvgaPlayback") {
                            "complete source=${source.logLabel()} frame=${state.currentFrame} completedIterations=${state.completedIterations}"
                        }
                        invalidateDraw()
                        break
                    }
                    state.completedIterations += 1
                    state.currentFrame = 0
                    audioController.onFrame(0)
                } else {
                    state.currentFrame = nextFrame
                    audioController.onFrame(nextFrame)
                }
                invalidateDraw()
            }
        }
    }

    private fun releaseRuntime(resetOverlay: Boolean) {
        SvgaLogger.debug("SvgaNode") {
            "release source=${source.logLabel()} resetOverlay=$resetOverlay"
        }
        loadJob = null
        playbackJob = null
        preparedMovie = null
        renderer = null
        state.hitRegions = emptyMap()
        audioController.stop()
        audioController = EmptySvgaAudioController
        if (resetOverlay) {
            slotState.overlay = SvgaOverlay.Loading
        }
        invalidateDraw()
    }

    private fun Size.hasSpecifiedAndFiniteWidth(): Boolean = this != Size.Unspecified && width.isFinite()

    private fun Size.hasSpecifiedAndFiniteHeight(): Boolean = this != Size.Unspecified && height.isFinite()

    private fun Constraints.hasFixedSize(): Boolean = hasFixedWidth && hasFixedHeight
}
