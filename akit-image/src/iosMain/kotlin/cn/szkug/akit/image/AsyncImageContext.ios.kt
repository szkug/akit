package cn.szkug.akit.image

import androidx.compose.runtime.ProvidableCompositionLocal
import coil3.compose.LocalPlatformContext

actual typealias PlatformContext = coil3.PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalPlatformContext

actual object DefaultPlatformAsyncImageLogger : AsyncImageLogger {

    const val DEBUG = 1
    const val INFO = 2
    const val WARN = 3
    const val ERROR = 4

    var level = ERROR

    actual override fun debug(tag: String, message: () -> String) {
        if (DEBUG < level) return
        log(tag, "DEBUG", message())
    }

    actual override fun info(tag: String, message: () -> String) {
        if (INFO < level) return
        log(tag, "INFO", message())
    }

    actual override fun warn(tag: String, message: String) {
        if (WARN < level) return
        log(tag, "WARN", message)
    }

    actual override fun error(tag: String, exception: Exception?) {
        if (ERROR < level || exception == null) return
        log(tag, "ERROR", "${exception::class.simpleName}: ${exception.message.orEmpty()}")
        log(tag, "ERROR", exception.stackTraceToString())
    }

    actual override fun error(tag: String, message: String) {
        if (ERROR < level) return
        log(tag, "ERROR", message)
    }

    private fun log(tag: String, level: String, message: String) {
        println("AkitAsyncImage [$tag][$level]: $message")
    }
}