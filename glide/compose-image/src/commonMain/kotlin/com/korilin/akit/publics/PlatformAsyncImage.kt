package com.korilin.akit.publics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale

@Composable
internal expect fun PlatformAsyncImage(
    model: Any?,
    placeholder: PainterModel?,
    failureRes: ResourceModel?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alignment: Alignment,
    alpha: Float,
    colorFilter: ColorFilter?,
    context: AsyncImageContext,
)

internal expect fun Modifier.platformAsyncBackground(
    model: Any?,
    placeholder: PainterModel?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    context: AsyncImageContext?,
): Modifier
