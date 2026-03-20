package munchkin.resources.loader.glide

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.EngineContextProvider
import munchkin.resources.loader.LocalEngineContextRegister
import munchkin.resources.loader.RequestEngine
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class GlideLoaderEngineContext(val androidContext: Context) : EngineContext

private val GlideEngineContextProvider: EngineContextProvider<GlideLoaderEngineContext> = @Composable {
    GlideLoaderEngineContext(LocalContext.current)
}

interface GlideContextRegisterEngine : RequestEngine<GlideLoaderEngineContext> {
    override val contextType: KClass<GlideLoaderEngineContext>
        get() = GlideLoaderEngineContext::class

    companion object {
        fun register() {
            LocalEngineContextRegister.register(
                GlideLoaderEngineContext::class,
                GlideEngineContextProvider,
            )
        }
    }
}
