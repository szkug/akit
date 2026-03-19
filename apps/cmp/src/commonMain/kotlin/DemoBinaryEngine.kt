package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.resources.loader.BinaryRequestEngine

@Composable
expect fun rememberDemoBinaryEngine(): BinaryRequestEngine
