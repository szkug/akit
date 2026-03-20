package munchkin.svga

import munchkin.resources.runtime.DefaultPlatformMunchkinLogger
import munchkin.resources.runtime.BinarySource

internal object SvgaLogger {
    fun debug(tag: String, message: () -> String) {
        DefaultPlatformMunchkinLogger.debug(tag, message)
    }

    fun info(tag: String, message: () -> String) {
        DefaultPlatformMunchkinLogger.info(tag, message)
    }

    fun error(tag: String, throwable: Throwable? = null, message: () -> String) {
        DefaultPlatformMunchkinLogger.error(tag, message())
        when (throwable) {
            null -> Unit
            is Exception -> DefaultPlatformMunchkinLogger.error(tag, throwable)
            else -> {
                DefaultPlatformMunchkinLogger.error(
                    tag,
                    "${throwable::class.simpleName}: ${throwable.message.orEmpty()}",
                )
                DefaultPlatformMunchkinLogger.error(tag, throwable.stackTraceToString())
            }
        }
    }
}

internal fun BinarySource.logLabel(): String {
    return when (this) {
        is munchkin.resources.runtime.BinarySource.Bytes -> "Bytes(cacheKey=$cacheKey, size=${value.size})"
        is munchkin.resources.runtime.BinarySource.FilePath -> "FilePath(path=$path)"
        is munchkin.resources.runtime.BinarySource.Raw -> "Raw(id=$id)"
        is munchkin.resources.runtime.BinarySource.UriPath -> "UriPath(value=$value)"
        is munchkin.resources.runtime.BinarySource.Url -> "Url(value=$value)"
    }
}
