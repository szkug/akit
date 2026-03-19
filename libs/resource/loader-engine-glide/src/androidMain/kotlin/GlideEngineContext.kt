package munchkin.resources.loader.glide

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.EngineContextProvider

@JvmInline
internal value class GlideLoaderEngineContext(val context: Context) : EngineContext

internal val EngineContext.androidContext: Context
    get() = (this as GlideLoaderEngineContext).context

internal val GlideEngineContextProvider: EngineContextProvider =
    { GlideLoaderEngineContext(LocalContext.current) }
