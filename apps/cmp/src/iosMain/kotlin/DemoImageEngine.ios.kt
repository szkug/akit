package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.ImageAsyncLoadData
import munchkin.resources.loader.ImageAsyncRequestEngine
import munchkin.resources.loader.coil.CoilImageRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): ImageAsyncRequestEngine<out EngineContext, ImageAsyncLoadData> = CoilImageRequestEngine.Normal
