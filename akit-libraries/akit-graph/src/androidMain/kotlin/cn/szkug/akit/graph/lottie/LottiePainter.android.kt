package cn.szkug.akit.graph.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import cn.szkug.akit.graph.EmptyPainter
import cn.szkug.akit.graph.toPainter
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

@Composable
actual fun rememberLottiePainter(resource: LottieResource): Painter {
    val resId = resource.resource as? Int ?: return EmptyPainter
    val context = LocalContext.current
    val composition = remember(context, resId) {
        LottieCompositionFactory.fromRawResSync(context, resId).value
    }
    val drawable = remember(composition, resource.iterations) {
        if (composition == null) return@remember null
        LottieDrawable().apply {
            setComposition(composition)
            repeatMode = LottieDrawable.RESTART
            repeatCount = if (resource.iterations < 0) LottieDrawable.INFINITE else resource.iterations
        }
    }
    return drawable?.toPainter() ?: EmptyPainter
}
