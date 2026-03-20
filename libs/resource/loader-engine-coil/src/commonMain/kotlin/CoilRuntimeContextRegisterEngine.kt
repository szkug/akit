package munchkin.resources.runtime.coil

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.RuntimeEngineContextProvider
import munchkin.resources.runtime.LocalRuntimeEngineContextRegister
import munchkin.resources.runtime.RuntimeRequestEngine
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class CoilRuntimeEngineContext(val context: coil3.PlatformContext) : RuntimeEngineContext

private val CoilRuntimeEngineContextProvider: RuntimeEngineContextProvider<CoilRuntimeEngineContext> = @Composable {
    CoilRuntimeEngineContext(coil3.compose.LocalPlatformContext.current)
}

interface CoilRuntimeContextRegisterEngine : RuntimeRequestEngine<CoilRuntimeEngineContext> {
    override val contextType: KClass<CoilRuntimeEngineContext> get() = CoilRuntimeEngineContext::class

    companion object {
        fun register() {
            LocalRuntimeEngineContextRegister.register(
                CoilRuntimeEngineContext::class,
                CoilRuntimeEngineContextProvider,
            )
        }
    }
}
