package akit.apps.cmp

import androidx.compose.runtime.Composable
import akit.image.AsyncRequestEngine
import akit.image.coil.CoilRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): AsyncRequestEngine<*> = CoilRequestEngine.Normal
