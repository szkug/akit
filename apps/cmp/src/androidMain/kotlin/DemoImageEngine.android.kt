package akit.apps.cmp

import androidx.compose.runtime.Composable
import akit.image.AsyncRequestEngine
import akit.image.glide.GlideRequestEngine
import akit.image.coil.CoilRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): AsyncRequestEngine<*> = GlideRequestEngine.Normal // GlideRequestEngine.Normal
