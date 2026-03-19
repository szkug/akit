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

typealias EngineContextProvider = @Composable () -> EngineContext

object LocalEngineContextRegister {

    private val registration = mutableMapOf<KClass<*>, EngineContextProvider>()

    fun register(type: KClass<*>, provider: EngineContextProvider) {
        registration[type] = provider
    }

    @Composable
    fun resolve(engine: Any): EngineContext {
        val provider = registration[engine::class]
            ?: error("No EngineContext provider found, it must register first.")
        return provider.invoke()
    }
}
