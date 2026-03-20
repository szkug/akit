package munchkin.image

import androidx.compose.runtime.Composable

typealias RuntimeEngineContext = munchkin.resources.runtime.RuntimeEngineContext

@Composable
internal expect fun Any?.platformResourceModel(): ResourceModel?

@Composable
internal expect fun Any?.platformPainterModel(): PainterModel?
