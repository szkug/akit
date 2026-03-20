package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.ImageAsyncLoadData
import munchkin.resources.runtime.RuntimeImageRequestEngine

@Composable
expect fun rememberDemoAsyncEngine(): RuntimeImageRequestEngine<out RuntimeEngineContext, ImageAsyncLoadData>
