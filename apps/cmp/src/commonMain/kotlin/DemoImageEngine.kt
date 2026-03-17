package munchkin.apps.cmp

import androidx.compose.runtime.Composable
import munchkin.image.AsyncRequestEngine

@Composable
expect fun rememberDemoAsyncEngine(): AsyncRequestEngine<*>
