package cn.szkug.akit.publics

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext

@Composable
internal actual fun rememberPlatformAsyncImageContextData(): Any? {
    val platformContext = LocalPlatformContext.current
    return rememberIosAsyncImageContextData(platformContext)
}
