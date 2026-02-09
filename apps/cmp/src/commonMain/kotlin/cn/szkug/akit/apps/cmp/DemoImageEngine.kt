package cn.szkug.akit.apps.cmp

import androidx.compose.runtime.Composable
import cn.szkug.akit.image.AsyncRequestEngine

@Composable
expect fun rememberDemoAsyncEngine(): AsyncRequestEngine<*>
