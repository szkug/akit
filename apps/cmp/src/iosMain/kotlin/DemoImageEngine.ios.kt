package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.ImageAsyncRequestEngine
import munchkin.resources.loader.coil.CoilImageRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): ImageAsyncRequestEngine<*> = CoilImageRequestEngine.Normal
