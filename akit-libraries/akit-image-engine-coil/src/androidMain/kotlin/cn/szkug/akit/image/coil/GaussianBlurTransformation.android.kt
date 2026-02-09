package cn.szkug.akit.image.coil

import cn.szkug.akit.graph.renderscript.BlurConfig
import cn.szkug.akit.graph.renderscript.Toolkit
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

internal actual class GaussianBlurTransformation actual constructor(
    private val config: BlurConfig,
) : Transformation() {

    actual override val cacheKey: String = "akit.blur.${config.radius}.${config.repeat}"

    actual override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input

        val radius = BlurConfig.coerceInMod(config.radius)
        val repeat = BlurConfig.coerceInMod(config.repeat)
        if (radius == 0 || repeat == 0) return input

        var blurred: Bitmap = input
        repeat(repeat) {
            val next = Toolkit.blur(blurred, radius, null)
            blurred = next
        }
        return blurred
    }
}
