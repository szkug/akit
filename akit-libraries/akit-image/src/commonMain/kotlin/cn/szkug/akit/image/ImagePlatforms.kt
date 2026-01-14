package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.graphics.painter.Painter

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
expect fun Any?.toResourceModel(): ResourceModel?

@Composable
expect fun Any?.toPainterModel(): PainterModel?