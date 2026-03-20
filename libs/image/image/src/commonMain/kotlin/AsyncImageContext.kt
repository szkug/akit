package munchkin.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import munchkin.graph.renderscript.BlurConfig
import munchkin.resources.runtime.DefaultPlatformMunchkinLogger
import munchkin.resources.runtime.LocalRuntimeEngineContextRegister as RuntimeContextRegister
import munchkin.resources.runtime.MunchkinLogger
import munchkin.resources.runtime.RuntimeImageLoadListener
import munchkin.resources.runtime.RuntimeImageRequestContext
import munchkin.resources.runtime.RuntimeImageSizeLimit
import kotlin.coroutines.CoroutineContext

typealias AsyncImageLogger = MunchkinLogger
typealias DefaultPlatformAsyncImageLogger = DefaultPlatformMunchkinLogger
typealias AsyncImageLoadListener = RuntimeImageLoadListener
typealias AsyncImageSizeLimit = RuntimeImageSizeLimit
typealias RuntimeEngineContextProvider<C> = munchkin.resources.runtime.RuntimeEngineContextProvider<C>
typealias LocalRuntimeEngineContextRegister = RuntimeContextRegister

class AsyncImageContext(
    val coroutineContext: CoroutineContext,
    override val logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    override val listener: AsyncImageLoadListener? = null,
    override val ignoreImagePadding: Boolean = false,
    override val animationIterations: Int = -1,
    override val blurConfig: BlurConfig? = null,
    override val sizeLimit: AsyncImageSizeLimit? = null,
    override val supportNinepatch: Boolean = false,
    override val supportLottie: Boolean = false,
) : RuntimeImageRequestContext {
    companion object
}

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
): AsyncImageContext {
    return remember(ignoreImagePadding, supportNinepatch, animationContext, blurConfig, sizeLimit, *keys) {
        AsyncImageContext(
            ignoreImagePadding = ignoreImagePadding,
            logger = logger,
            listener = listener,
            coroutineContext = animationContext,
            blurConfig = blurConfig,
            sizeLimit = sizeLimit,
            supportNinepatch = supportNinepatch,
            supportLottie = false,
        )
    }
}
