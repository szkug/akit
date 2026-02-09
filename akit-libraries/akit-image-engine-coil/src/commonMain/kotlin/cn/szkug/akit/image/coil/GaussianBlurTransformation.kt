package cn.szkug.akit.image.coil

import cn.szkug.akit.graph.renderscript.BlurConfig
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

internal expect class GaussianBlurTransformation(
    config: BlurConfig,
) : Transformation {
    override val cacheKey: String
    override suspend fun transform(input: Bitmap, size: Size): Bitmap
}
