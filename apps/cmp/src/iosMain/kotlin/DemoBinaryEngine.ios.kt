package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.EngineContext
import munchkin.resources.loader.SvgaAsyncRequestEngine
import munchkin.resources.loader.coil.CoilSvgaRequestEngine

@Composable
actual fun rememberDemoBinaryEngine(): SvgaAsyncRequestEngine<out EngineContext> = CoilSvgaRequestEngine.Normal
