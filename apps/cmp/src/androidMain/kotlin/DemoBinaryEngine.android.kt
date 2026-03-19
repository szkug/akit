package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.BinaryRequestEngine
import munchkin.resources.loader.glide.GlideBinaryRequestEngine

@Composable
actual fun rememberDemoBinaryEngine(): BinaryRequestEngine = GlideBinaryRequestEngine.Normal
