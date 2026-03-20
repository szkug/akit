package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RuntimeEngineContext
import munchkin.resources.runtime.RuntimeSvgaRequestEngine

@Composable
expect fun rememberDemoBinaryEngine(): RuntimeSvgaRequestEngine<out RuntimeEngineContext>
