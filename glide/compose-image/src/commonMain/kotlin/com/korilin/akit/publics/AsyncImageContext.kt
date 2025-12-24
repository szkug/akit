package com.korilin.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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
    return remember(enableLog, ignoreImagePadding, *keys) {
        AsyncImageContext(
            enableLog = enableLog,
            ignoreImagePadding = ignoreImagePadding,
        )
    }
}
