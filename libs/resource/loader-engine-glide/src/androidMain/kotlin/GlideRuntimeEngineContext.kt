package munchkin.resources.runtime.glide

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.RuntimeEngineContextProvider
import munchkin.resources.runtime.LocalRuntimeEngineContextRegister
import munchkin.resources.runtime.RuntimeRequestEngine
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class GlideRuntimeEngineContext(val androidContext: Context) : RuntimeEngineContext

private val GlideRuntimeEngineContextProvider: RuntimeEngineContextProvider<GlideRuntimeEngineContext> = @Composable {
    GlideRuntimeEngineContext(LocalContext.current)
}

interface GlideRuntimeContextRegisterEngine : RuntimeRequestEngine<GlideRuntimeEngineContext> {
    override val contextType: KClass<GlideRuntimeEngineContext>
        get() = GlideRuntimeEngineContext::class

    companion object {
        fun register() {
            LocalRuntimeEngineContextRegister.register(
                GlideRuntimeEngineContext::class,
                GlideRuntimeEngineContextProvider,
            )
        }
    }
}
