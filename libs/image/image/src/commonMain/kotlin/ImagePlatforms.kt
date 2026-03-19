package munchkin.image

import androidx.compose.runtime.Composable

typealias EngineContext = munchkin.resources.loader.EngineContext

@Composable
internal expect fun Any?.platformResourceModel(): ResourceModel?

@Composable
internal expect fun Any?.platformPainterModel(): PainterModel?
