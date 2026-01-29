package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.lottie.LottieResource
import cn.szkug.akit.graph.lottie.rememberLottiePainter
import cn.szkug.akit.resources.runtime.ResourceId
import cn.szkug.akit.resources.runtime.painterResource

expect val SDK_SIZE_ORIGINAL: Int

expect abstract class PlatformImageContext

expect val LocalPlatformImageContext: ProvidableCompositionLocal<PlatformImageContext>

expect object DefaultPlatformAsyncImageLogger : AsyncImageLogger {
    override fun setLevel(level: AsyncImageLogger.Level)
    override fun debug(tag: String, message: () -> String)
    override fun info(tag: String, message: () -> String)
    override fun warn(tag: String, message: String)
    override fun error(tag: String, exception: Exception?)
    override fun error(tag: String, message: String)
}


expect class PlatformAsyncLoadData : AsyncLoadData {
    override fun painter(): Painter
}

expect val LocalPlatformAsyncRequestEngine: ProvidableCompositionLocal<AsyncRequestEngine<PlatformAsyncLoadData>>


@Composable
expect fun Any?.platformResourceModel(): ResourceModel?

@Composable
expect fun Any?.platformPainterModel(): PainterModel?
