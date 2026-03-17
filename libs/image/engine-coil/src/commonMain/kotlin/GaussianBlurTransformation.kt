package munchkin.image.coil.support

import munchkin.graph.renderscript.BlurConfig
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

internal expect class GaussianBlurTransformation(
    config: BlurConfig,
) : Transformation {
    override val cacheKey: String
    override suspend fun transform(input: Bitmap, size: Size): Bitmap
}
