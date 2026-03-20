package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.RuntimeSvgaRequestEngine
import munchkin.resources.runtime.glide.GlideRuntimeSvgaRequestEngine

@Composable
actual fun rememberDemoBinaryEngine(): RuntimeSvgaRequestEngine<out RuntimeEngineContext> = GlideRuntimeSvgaRequestEngine.Normal
