package cn.szkug.akit.graph.lottie

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

data class LottieResource(
    val resource: Any,
    val iterations: Int = LottieIterations.Forever,
)

object LottieIterations {
    const val Forever: Int = -1
}

@Composable
expect fun rememberLottiePainter(resource: LottieResource): Painter
