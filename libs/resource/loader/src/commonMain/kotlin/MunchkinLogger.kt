package munchkin.resources.loader

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

interface MunchkinLogger {

    enum class Level {
        DEBUG, INFO, WARN, ERROR,
    }

    fun setLevel(level: Level)
    fun debug(feature: String, message: () -> String)
    fun info(feature: String, message: () -> String)
    fun warn(feature: String, message: String)
    fun error(feature: String, exception: Exception?)
    fun error(feature: String, message: String)
}

expect object DefaultPlatformMunchkinLogger : MunchkinLogger {
    override fun setLevel(level: MunchkinLogger.Level)
    override fun debug(feature: String, message: () -> String)
    override fun info(feature: String, message: () -> String)
    override fun warn(feature: String, message: String)
    override fun error(feature: String, exception: Exception?)
    override fun error(feature: String, message: String)
}

interface EngineContext

typealias EngineContextProvider<C> = @Composable () -> C

interface RequestEngine<C : EngineContext> {
    val contextType: KClass<C>
}

object LocalEngineContextRegister {

    private data class Registration<C : EngineContext>(
        val provider: EngineContextProvider<C>,
    )

    private val registration =
        mutableMapOf<KClass<out EngineContext>, Registration<out EngineContext>>()

    fun <C : EngineContext> register(
        type: KClass<C>,
        provider: EngineContextProvider<C>,
    ) {
        registration[type] = Registration(provider)
    }

    @Composable
    fun <C : EngineContext> resolve(engine: RequestEngine<C>): C {
        @Suppress("UNCHECKED_CAST")
        val resolved = registration[engine.contextType] as? Registration<C>
        return resolved?.provider?.invoke()
            ?: error("No EngineContext type ${engine.contextType.simpleName} provider found, it must register first.")
    }
}
