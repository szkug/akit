package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

expect class ResourceId

@Composable
expect fun stringResource(id: ResourceId, vararg formatArgs: Any): String

@Composable
expect fun pluralStringResource(id: ResourceId, vararg formatArgs: Any): String

@Composable
expect fun colorResource(id: ResourceId): Color
@Composable
expect fun painterResource(id: ResourceId): Painter

@get:Composable
expect val ResourceId.toDp: Dp

@get:Composable
expect val ResourceId.toSp: TextUnit


expect fun resolveResourcePath(id: ResourceId, localeOverride: String? = null): String?
