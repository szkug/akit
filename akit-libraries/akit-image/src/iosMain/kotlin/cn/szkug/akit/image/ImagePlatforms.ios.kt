package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asComposeImageBitmap

@Composable
actual fun Any?.platformResourceModel(): ResourceModel? = when (this) {
    else -> null
}

@Composable
actual fun Any?.platformPainterModel(): PainterModel? = when (this) {
    else -> null
}


actual object DefaultPlatformAsyncImageLogger : AsyncImageLogger {

    private var level = AsyncImageLogger.Level.ERROR

    actual override fun setLevel(level: AsyncImageLogger.Level) {
        this.level = level
    }

    actual override fun debug(tag: String, message: () -> String) {
        if (AsyncImageLogger.Level.DEBUG < level) return
        log(tag, "DEBUG", message())
    }

    actual override fun info(tag: String, message: () -> String) {
        if (AsyncImageLogger.Level.INFO < level) return
        log(tag, "INFO", message())
    }

    actual override fun warn(tag: String, message: String) {
        if (AsyncImageLogger.Level.WARN < level) return
        log(tag, "WARN", message)
    }

    actual override fun error(tag: String, exception: Exception?) {
        if (AsyncImageLogger.Level.ERROR < level || exception == null) return
        log(tag, "ERROR", "${exception::class.simpleName}: ${exception.message.orEmpty()}")
        log(tag, "ERROR", exception.stackTraceToString())
    }

    actual override fun error(tag: String, message: String) {
        if (AsyncImageLogger.Level.ERROR < level) return
        log(tag, "ERROR", message)
    }

    private fun log(tag: String, level: String, message: String) {
        println("AkitAsyncImage [$tag][$level]: $message")
    }
}
