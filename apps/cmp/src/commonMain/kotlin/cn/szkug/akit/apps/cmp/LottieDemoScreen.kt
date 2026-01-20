package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.szkug.akit.image.AkitAsyncImage
import cn.szkug.akit.lottie.LottieResource

@Composable
fun LottieDemoPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "Lottie Demo", onBack = onBack)
        LottieDemoScreen(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun LottieDemoScreen(modifier: Modifier = Modifier) {
    val samples = listOf(
        LottieSample("Pulse 1", LottieResource(Res.raw.lottie_pulse)),
        LottieSample("Pulse 2", LottieResource(Res.raw.lottie_pulse)),
        LottieSample("Spinner", LottieResource(Res.raw.lottie_spinner)),
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(samples) { sample ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = sample.title)
                AkitAsyncImage(
                    model = sample.resource,
                    contentDescription = sample.title,
                    modifier = Modifier.size(160.dp),

                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private data class LottieSample(
    val title: String,
    val resource: Any,
)
