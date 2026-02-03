package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

expect abstract class ResourceId
expect class StringResourceId : ResourceId
expect class PluralStringResourceId : ResourceId
expect class ColorResourceId : ResourceId
expect class RawResourceId : ResourceId
expect class ImageResourceId : ResourceId
expect class DimenResourceId : ResourceId

@Composable
expect fun stringResource(id: StringResourceId, vararg formatArgs: Any): String

@Composable
expect fun pluralStringResource(
    id: PluralStringResourceId,
    count: Int,
    vararg formatArgs: Any
): String

@Composable
expect fun colorResource(id: ColorResourceId): Color

@Composable
expect fun painterResource(id: ImageResourceId): Painter

@get:Composable
expect val DimenResourceId.toDp: Dp

@get:Composable
expect val DimenResourceId.toSp: TextUnit

expect fun resolveResourcePath(id: ResourceId, localeOverride: String? = null): String?
