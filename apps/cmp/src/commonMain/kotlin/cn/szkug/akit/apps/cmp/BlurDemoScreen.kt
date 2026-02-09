package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.graph.renderscript.BlurConfig
import cn.szkug.akit.image.AkitAsyncImage
import cn.szkug.akit.image.rememberAsyncImageContext
import kotlin.math.roundToInt

@Composable
fun BlurDemoPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "Gaussian Blur", onBack = onBack)
        BlurDemoScreen(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun BlurDemoScreen(modifier: Modifier = Modifier) {
    var enabled by remember { mutableStateOf(true) }
    var radiusValue by remember { mutableStateOf(12f) }
    var repeatValue by remember { mutableStateOf(1f) }
    val engine = rememberDemoAsyncEngine()

    val radius = radiusValue.roundToInt().coerceIn(0, 25)
    val repeat = repeatValue.roundToInt().coerceIn(1, 5)
    val blurConfig = if (enabled) BlurConfig(radius = radius, repeat = repeat) else null
    val blurContext = rememberAsyncImageContext(blurConfig = blurConfig)

    Column(
        modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Blur Controls")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Enabled")
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        Text(text = "Radius: $radius")
        Slider(
            value = radiusValue,
            onValueChange = { radiusValue = it },
            valueRange = 0f..25f
        )
        Text(text = "Repeat: $repeat")
        Slider(
            value = repeatValue,
            onValueChange = { repeatValue = it },
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Original")
        AkitAsyncImage(
            model = DemoUrls.urls.first(),
            contentDescription = "original",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Crop,
            engine = engine,
        )

        Text(text = "Blurred")
        AkitAsyncImage(
            model = DemoUrls.urls.first(),
            contentDescription = "blurred",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Crop,
            context = blurContext,
            engine = engine,
        )

        Spacer(modifier = Modifier.size(8.dp))
    }
}
