package munchkin.resources.runtime

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

interface RuntimeEngineContext

typealias RuntimeEngineContextProvider<C> = @Composable () -> C

interface RuntimeRequestEngine<C : RuntimeEngineContext> {
    val contextType: KClass<C>
}

object LocalRuntimeEngineContextRegister {

    private data class Registration<C : RuntimeEngineContext>(
        val provider: RuntimeEngineContextProvider<C>,
    )

    private val registration =
        mutableMapOf<KClass<out RuntimeEngineContext>, Registration<out RuntimeEngineContext>>()

    fun <C : RuntimeEngineContext> register(
        type: KClass<C>,
        provider: RuntimeEngineContextProvider<C>,
    ) {
        registration[type] = Registration(provider)
    }

    @Composable
    fun <C : RuntimeEngineContext> resolve(engine: RuntimeRequestEngine<C>): C {
        @Suppress("UNCHECKED_CAST")
        val resolved = registration[engine.contextType] as? Registration<C>
        return resolved?.provider?.invoke()
            ?: error("No RuntimeEngineContext type ${engine.contextType.simpleName} provider found, it must register first.")
    }
}
