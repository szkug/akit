package akit.apps.cmp

import androidx.compose.runtime.Composable
import akit.image.AsyncRequestEngine

@Composable
expect fun rememberDemoAsyncEngine(): AsyncRequestEngine<*>
