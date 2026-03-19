package munchkin.image

import androidx.compose.runtime.Composable

@Composable
actual fun Any?.platformResourceModel(): ResourceModel? = when (this) {
    else -> null
}

@Composable
actual fun Any?.platformPainterModel(): PainterModel? = when (this) {
    else -> null
}
