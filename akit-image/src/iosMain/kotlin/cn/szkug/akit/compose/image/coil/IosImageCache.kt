package cn.szkug.akit.compose.image.coil

import androidx.compose.ui.graphics.ImageBitmap
import cn.szkug.graphics.ninepatch.NinePatchChunk

internal data class IosCachedImage(
    val image: ImageBitmap,
    val chunk: NinePatchChunk?,
)
