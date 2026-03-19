package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.ImageAsyncRequestEngine
import munchkin.resources.loader.glide.GlideImageRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): ImageAsyncRequestEngine<*> = GlideImageRequestEngine.Normal
