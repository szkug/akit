package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.BinaryRequestEngine
import munchkin.resources.loader.coil.CoilBinaryRequestEngine

@Composable
actual fun rememberDemoBinaryEngine(): BinaryRequestEngine = CoilBinaryRequestEngine.Normal
