package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.image.AsyncRequestEngine
import munchkin.image.coil.CoilRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): AsyncRequestEngine<*> = CoilRequestEngine.Normal
