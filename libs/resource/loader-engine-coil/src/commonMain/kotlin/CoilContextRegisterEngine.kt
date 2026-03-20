package munchkin.resources.loader.coil

import androidx.compose.runtime.Composable
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.EngineContextProvider
import munchkin.resources.loader.LocalEngineContextRegister
import munchkin.resources.loader.RequestEngine
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class CoilEngineContext(val context: coil3.PlatformContext) : EngineContext

private val CoilEngineContextProvider: EngineContextProvider<CoilEngineContext> = @Composable {
    CoilEngineContext(coil3.compose.LocalPlatformContext.current)
}

interface CoilContextRegisterEngine : RequestEngine<CoilEngineContext> {
    override val contextType: KClass<CoilEngineContext> get() = CoilEngineContext::class

    companion object {
        fun register() {
            LocalEngineContextRegister.register(
                CoilEngineContext::class,
                CoilEngineContextProvider,
            )
        }
    }
}
