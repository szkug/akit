package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.intl.LocaleList
import cn.szkug.akit.graph.renderscript.BlurConfig
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass


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
    val coroutineContext: CoroutineContext,

    val logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    val listener: AsyncImageLoadListener? = null,
    val ignoreImagePadding: Boolean = false,

    val animationIterations: Int = -1,
    val blurConfig: BlurConfig? = null,

    // extension support
    val supportNinepatch: Boolean = false,
    val supportLottie: Boolean = false,
) {
    companion object
}

@Composable
fun rememberAsyncImageContext(
    ignoreImagePadding: Boolean = false,
    logger: AsyncImageLogger = DefaultPlatformAsyncImageLogger,
    listener: AsyncImageLoadListener? = null,
    animationContext: CoroutineContext = rememberCoroutineScope().coroutineContext,
    blurConfig: BlurConfig? = null,
    // extension support
    supportNinepatch: Boolean = false,
    vararg keys: Any?,
): AsyncImageContext {

    return remember(ignoreImagePadding, supportNinepatch, animationContext, blurConfig, *keys) {
        AsyncImageContext(
            ignoreImagePadding = ignoreImagePadding,
            logger = logger,
            listener = listener,
            coroutineContext = animationContext,
            blurConfig = blurConfig,
            supportNinepatch = supportNinepatch
        )
    }
}



typealias EngineContextProvider = @Composable () -> EngineContext
object LocalEngineContextRegister {

    private val registration = mutableMapOf<KClass<out AsyncRequestEngine<*>>, EngineContextProvider>()

    fun register(type: KClass<out AsyncRequestEngine<*>>, provider: EngineContextProvider) {
        registration[type] = provider
    }

    @Composable
    fun resolve(engine: AsyncRequestEngine<*>): EngineContext {
        val provider = registration[engine::class]
            ?: error("No EngineContext provider found, it must register first.")
        return provider.invoke()
    }
}