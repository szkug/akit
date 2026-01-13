package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

expect class ResourceId

@Composable
expect fun stringResource(id: ResourceId, vararg formatArgs: Any): String

@Composable
expect fun painterResource(id: ResourceId): Painter
