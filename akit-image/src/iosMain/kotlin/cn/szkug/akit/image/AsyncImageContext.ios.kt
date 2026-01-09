package cn.szkug.akit.image

import androidx.compose.runtime.ProvidableCompositionLocal
import coil3.compose.LocalPlatformContext

actual typealias PlatformContext = coil3.PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalPlatformContext

actual object DefaultPlatformAsyncImageLogger : AsyncImageLogger {
    actual override fun debug(tag: String, message: () -> String) {
    }

    actual override fun info(tag: String, message: () -> String) {
    }

    actual override fun warn(tag: String, message: String) {
    }

    actual override fun error(tag: String, exception: Exception?) {
    }

    actual override fun error(tag: String, message: String) {
    }
}