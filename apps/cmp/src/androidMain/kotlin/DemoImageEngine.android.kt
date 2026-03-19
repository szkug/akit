package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.image.AsyncRequestEngine
import munchkin.image.glide.GlideRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): AsyncRequestEngine<*> = GlideRequestEngine.Normal // GlideRequestEngine.Normal
