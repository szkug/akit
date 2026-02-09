package cn.szkug.akit.apps.cmp

import androidx.compose.runtime.Composable
import cn.szkug.akit.image.AsyncRequestEngine
import cn.szkug.akit.image.coil.CoilRequestEngine

@Composable
actual fun rememberDemoAsyncEngine(): AsyncRequestEngine<*> = CoilRequestEngine.Normal
