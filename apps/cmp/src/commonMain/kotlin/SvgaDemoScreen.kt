package munchkin.apps.cmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import munchkin.resources.runtime.DefaultPlatformMunchkinLogger
import munchkin.resources.runtime.MunchkinLogger
import munchkin.resources.runtime.BinarySource
import munchkin.svga.MunchkinSvga
import munchkin.svga.SvgaDynamicEntity
import munchkin.svga.rememberSvgaPlayerState

/**
 * Hosts the SVGA demo page inside the sample app and delegates the actual showcase content to
 * [SvgaDemoScreen].
 */
@Composable
fun SvgaDemoPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "SVGA Demo", onBack = onBack)
        SvgaDemoScreen(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Demonstrates the current SVGA runtime capabilities in one place.
 *
 * Covered scenarios:
 * - matte and click-hit testing
 * - dynamic text and custom draw callback replacement
 * - vector-only `movie.spec` playback
 * - audio timeline metadata playback
 * - repeated rendering of the wear sample used by the business demo module
 */
@Composable
private fun SvgaDemoScreen(modifier: Modifier = Modifier) {
    DefaultPlatformMunchkinLogger.setLevel(MunchkinLogger.Level.DEBUG)
    val loaderEngine = rememberDemoBinaryEngine()
    var clickMessage by remember { mutableStateOf("Tap the rocket body or right wing.") }

    /**
     * Builds the dynamic replacement set for the rocket sample so the demo can verify:
     * - hidden sprite handling
     * - dynamic text replacement
     * - custom drawing over a sprite slot
     * - click area dispatch
     */
    val rocketDynamic = remember {
        SvgaDynamicEntity().apply {
            setHidden(true, "rightlight")
            setDynamicText(
                text = "MUNCHKIN",
                forKey = "body",
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            )
            setDynamicDrawer(forKey = "left") { context ->
                drawCircle(
                    color = Color(0xFFFFA726),
                    radius = context.layout.width.coerceAtLeast(context.layout.height) * 0.18f,
                    center = androidx.compose.ui.geometry.Offset(
                        context.layout.x + context.layout.width * 0.75f,
                        context.layout.y + context.layout.height * 0.25f,
                    ),
                )
                false
            }
            setClickArea("body") { clickMessage = "Rocket body tapped." }
            setClickArea("right") { clickMessage = "Right wing tapped." }
        }
    }

    /**
     * Adds a lightweight overlay drawer to the vector sample to verify that custom draw callbacks
     * also work when the source asset is shape-driven instead of bitmap-driven.
     */
    val vectorDynamic = remember {
        SvgaDynamicEntity().apply {
            setDynamicDrawer(forKey = "31.vector") { _ ->
                drawRect(color = Color(0xFF29B6F6), alpha = 0.2f)
                false
            }
        }
    }

    /**
     * Keeps the demo scenarios data-driven so the sample list can render:
     * - feature validation samples first
     * - repeated wear assets afterwards for stress observation in the scrolling list
     */
    val samples = listOf(
        SvgaSample(
            title = "Rocket / matte / dynamic text / click",
            description = clickMessage,
            source = BinarySource.Raw(Res.raw.demo_svga_rocket),
            dynamic = rocketDynamic,
            iterations = -1,
        ),
        SvgaSample(
            title = "Vector path animation",
            description = "Pure movie.spec vector layers from the official sample.",
            source = BinarySource.Raw(Res.raw.demo_svga_vector),
            dynamic = vectorDynamic,
            iterations = -1,
        ),
        SvgaSample(
            title = "Audio timeline asset",
            description = "Audio-enabled SVGA sample used to validate binary decode and timeline metadata.",
            source = BinarySource.Raw(Res.raw.demo_svga_audio),
            iterations = 1,
        )
    ) + List(20) {
        SvgaSample(
            title = "Wear",
            description = "",
            source = BinarySource.Raw(Res.raw.demo_svga_wear),
            iterations = -1,
        ) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(samples) { sample ->
            val playerState = rememberSvgaPlayerState(iterations = sample.iterations)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = sample.title)
                Text(text = sample.description, color = Color.Gray)
                MunchkinSvga(
                    source = sample.source,
                    contentDescription = sample.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    state = playerState,
                    dynamicEntity = sample.dynamic ?: SvgaDynamicEntity.EMPTY,
                    loaderEngine = loaderEngine,
                    contentScale = ContentScale.Fit,
                    placeholder = {
                        Text(text = "Loading SVGA...", modifier = Modifier.padding(12.dp))
                    },
                    failure = { error ->
                        Text(text = error.message ?: "SVGA decode failed.", color = Color.Red)
                    },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Describes one SVGA showcase card rendered by [SvgaDemoScreen].
 */
private data class SvgaSample(
    /** Human-readable title shown above the sample player. */
    val title: String,
    /** Additional sample description or runtime status text. */
    val description: String,
    /** Binary source passed to the SVGA runtime. */
    val source: BinarySource,
    /** Optional dynamic replacement set used by the sample. */
    val dynamic: SvgaDynamicEntity? = null,
    /** Playback iteration count; `-1` means loop forever. */
    val iterations: Int = -1,
)
