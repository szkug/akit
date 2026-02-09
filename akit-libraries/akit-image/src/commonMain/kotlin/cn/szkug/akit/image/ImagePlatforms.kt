package cn.szkug.akit.image

import androidx.compose.runtime.Composable


interface EngineContext

expect object DefaultPlatformAsyncImageLogger : AsyncImageLogger {
    override fun setLevel(level: AsyncImageLogger.Level)
    override fun debug(tag: String, message: () -> String)
    override fun info(tag: String, message: () -> String)
    override fun warn(tag: String, message: String)
    override fun error(tag: String, exception: Exception?)
    override fun error(tag: String, message: String)
}


@Composable
internal expect fun Any?.platformResourceModel(): ResourceModel?

@Composable
internal expect fun Any?.platformPainterModel(): PainterModel?
