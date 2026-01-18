package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import cn.szkug.akit.graph.akitShadow
import kotlin.math.roundToInt

@Composable
fun ShadowEffectDemoPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "Shadow Effects", onBack = onBack)
        ShadowEffectDemoScreen(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ShadowEffectDemoScreen(modifier: Modifier = Modifier) {
    val samples = listOf(
        ShadowSample(
            title = "Soft Drop",
            shapeLabel = "Rounded(20)",
            shape = RoundedCornerShape(20.dp),
            shadowColor = Color(0x55000000),
            spreadAngle = 0f,
            effect = 16.dp,
            offset = DpOffset(0.dp, 8.dp),
            surface = Color(0xFFF7F7F7),
        ),
        ShadowSample(
            title = "Long Angle",
            shapeLabel = "Rounded(6)",
            shape = RoundedCornerShape(6.dp),
            shadowColor = Color(0x66000000),
            spreadAngle = 28f,
            effect = 36.dp,
            offset = DpOffset(10.dp, 12.dp),
            surface = Color(0xFFF2F1ED),
        ),
        ShadowSample(
            title = "Wide Fan",
            shapeLabel = "Rectangle",
            shape = RectangleShape,
            shadowColor = Color(0x5500618B),
            spreadAngle = 80f,
            effect = 28.dp,
            offset = DpOffset(0.dp, 12.dp),
            surface = Color(0xFFF1F6FA),
        ),
        ShadowSample(
            title = "Glow Ring",
            shapeLabel = "Circle",
            shape = CircleShape,
            shadowColor = Color(0x6626A69A),
            spreadAngle = 360f,
            effect = 22.dp,
            offset = DpOffset(0.dp, 0.dp),
            surface = Color(0xFFF3F9F8),
        ),
        ShadowSample(
            title = "Upward Lift",
            shapeLabel = "Rounded(12)",
            shape = RoundedCornerShape(12.dp),
            shadowColor = Color(0x552F2F2F),
            spreadAngle = 12f,
            effect = 14.dp,
            offset = DpOffset(0.dp, -6.dp),
            surface = Color(0xFFF7F5F0),
        ),
        ShadowSample(
            title = "Cut Corner",
            shapeLabel = "CutCorner(14)",
            shape = CutCornerShape(14.dp),
            shadowColor = Color(0x668A3A0A),
            spreadAngle = 8f,
            effect = 18.dp,
            offset = DpOffset(8.dp, 6.dp),
            surface = Color(0xFFF8F3EE),
        ),
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(samples) { sample ->
            ShadowSampleItem(sample = sample)
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ShadowSampleItem(sample: ShadowSample) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Text(text = sample.title)
        Text(text = sample.description())
        Box(
            modifier = Modifier
                .size(140.dp)
                .akitShadow(
                    shape = sample.shape,
                    color = sample.shadowColor,
                    spreadAngle = sample.spreadAngle,
                    effect = sample.effect,
                    offset = sample.offset,
                )
                .clip(sample.shape)
                .background(sample.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Akit")
        }
    }
}

private data class ShadowSample(
    val title: String,
    val shapeLabel: String,
    val shape: Shape,
    val shadowColor: Color,
    val spreadAngle: Float,
    val effect: Dp,
    val offset: DpOffset,
    val surface: Color,
)

private fun ShadowSample.description(): String {
    val effectText = "${effect.value.roundToInt()}dp"
    val offsetText = "(${offset.x.value.roundToInt()}dp, ${offset.y.value.roundToInt()}dp)"
    val angleText = "${spreadAngle.roundToInt()}deg"
    return "shape=$shapeLabel, spread=$angleText, effect=$effectText, offset=$offsetText"
}
