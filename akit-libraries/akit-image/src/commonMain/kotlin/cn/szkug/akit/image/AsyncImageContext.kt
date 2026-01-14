package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.coroutines.CoroutineContext


interface AsyncImageLogger {

    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    fun setLevel(level: Level)
    fun debug(tag: String, message: () -> String)
    fun info(tag: String, message: () -> String)
    fun warn(tag: String, message: String)
    fun error(tag: String, exception: Exception?)
    fun error(tag: String, message: String)
}


interface AsyncImageLoadListener {
    fun onStart(model: Any?) {}
    fun onSuccess(model: Any?) {}
    fun onFailure(model: Any?, exception: Throwable) {}
    fun onCancel(model: Any?) {}
}

class AsyncImageContext(
    val context: PlatformImageContext,
    val coroutineContext: CoroutineContext,

    val logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    val listener: AsyncImageLoadListener? = null,
    val ignoreImagePadding: Boolean = false,

    // extension support
    val supportNinepatch: Boolean = false,
) {
    companion object
}

@Composable
fun rememberAsyncImageContext(
    vararg keys: Any?,
    ignoreImagePadding: Boolean = false,
    logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    animationContext: CoroutineContext = rememberCoroutineScope().coroutineContext,
    // extension support
    supportNinepatch: Boolean = false,
): AsyncImageContext {

    val platformContext = LocalPlatformImageContext.current
    return remember(ignoreImagePadding, supportNinepatch, animationContext, *keys) {
        AsyncImageContext(
            context = platformContext,
            ignoreImagePadding = ignoreImagePadding,
            logger = logger,
            coroutineContext = animationContext,
            supportNinepatch = supportNinepatch
        )
    }
}
