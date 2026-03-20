package munchkin.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import munchkin.graph.renderscript.BlurConfig
import munchkin.resources.loader.AsyncImageContext as LoaderAsyncImageContext
import munchkin.resources.loader.AsyncImageLoadListener as LoaderAsyncImageLoadListener
import munchkin.resources.loader.AsyncImageSizeLimit as LoaderAsyncImageSizeLimit
import munchkin.resources.loader.DefaultPlatformMunchkinLogger
import munchkin.resources.loader.LocalEngineContextRegister as LoaderLocalEngineContextRegister
import munchkin.resources.loader.MunchkinLogger
import kotlin.coroutines.CoroutineContext

typealias AsyncImageLogger = MunchkinLogger
typealias DefaultPlatformAsyncImageLogger = DefaultPlatformMunchkinLogger
typealias AsyncImageLoadListener = LoaderAsyncImageLoadListener
typealias AsyncImageSizeLimit = LoaderAsyncImageSizeLimit
typealias AsyncImageContext = LoaderAsyncImageContext
typealias EngineContextProvider<C> = munchkin.resources.loader.EngineContextProvider<C>
typealias LocalEngineContextRegister = LoaderLocalEngineContextRegister

@Composable
fun rememberAsyncImageContext(
    ignoreImagePadding: Boolean = false,
    logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    listener: AsyncImageLoadListener? = null,
    animationContext: CoroutineContext = rememberCoroutineScope().coroutineContext,
    blurConfig: BlurConfig? = null,
    sizeLimit: AsyncImageSizeLimit? = null,
    supportNinepatch: Boolean = false,
    vararg keys: Any?,
): AsyncImageContext = munchkin.resources.loader.rememberAsyncImageContext(
    ignoreImagePadding = ignoreImagePadding,
    logger = logger,
    listener = listener,
    animationContext = animationContext,
    blurConfig = blurConfig,
    sizeLimit = sizeLimit,
    supportNinepatch = supportNinepatch,
    *keys,
)
