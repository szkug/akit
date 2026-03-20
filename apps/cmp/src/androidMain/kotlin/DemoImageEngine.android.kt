package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.ImageAsyncLoadData
import munchkin.resources.runtime.RuntimeImageRequestEngine
import munchkin.resources.runtime.glide.GlideRuntimeImageRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): RuntimeImageRequestEngine<out RuntimeEngineContext, ImageAsyncLoadData> = GlideRuntimeImageRequestEngine.Normal
