package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.SvgaAsyncRequestEngine

@Composable
expect fun rememberDemoBinaryEngine(): SvgaAsyncRequestEngine
