package cn.szkug.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

internal data class IosAsyncImageContextData(
    val platformContext: PlatformContext,
    val imageLoader: ImageLoader,
)

internal fun AsyncImageContext.requireIosData(): IosAsyncImageContextData {
    return platformData as? IosAsyncImageContextData
        ?: error("IosAsyncImageContextData is missing.")
}

internal fun AsyncImageContext.withIosData(data: IosAsyncImageContextData): AsyncImageContext {
    return AsyncImageContext(
        enableLog = enableLog,
        ignoreImagePadding = ignoreImagePadding,
        platformData = data,
    )
}

@Composable
internal fun rememberIosAsyncImageContextData(platformContext: PlatformContext): IosAsyncImageContextData {
    return remember(platformContext) {
        IosAsyncImageContextData(
            platformContext = platformContext,
            imageLoader = SingletonImageLoader.get(platformContext),
        )
    }
}
