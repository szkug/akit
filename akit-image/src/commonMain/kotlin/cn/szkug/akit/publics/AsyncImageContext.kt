package cn.szkug.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal expect fun rememberPlatformAsyncImageContextData(): Any?

open class AsyncImageContext(
    val enableLog: Boolean = false,
    val ignoreImagePadding: Boolean = false,
    internal val platformData: Any? = null,
) {
    companion object
}

@Composable
fun rememberAsyncImageContext(
    vararg keys: Any?,
    enableLog: Boolean = false,
    ignoreImagePadding: Boolean = false,
): AsyncImageContext {
    val platformData = rememberPlatformAsyncImageContextData()
    return remember(enableLog, ignoreImagePadding, platformData, *keys) {
        AsyncImageContext(
            enableLog = enableLog,
            ignoreImagePadding = ignoreImagePadding,
            platformData = platformData,
        )
    }
}
