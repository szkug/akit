package cn.szkug.akit.image.coil

import cn.szkug.akit.graph.renderscript.Toolkit
import cn.szkug.akit.graph.renderscript.BlurConfig
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.jetbrains.skia.ImageInfo

internal class GaussianBlurTransformation(
    private val config: BlurConfig,
) : Transformation() {

    override val cacheKey: String = "akit.blur.${config.radius}.${config.repeat}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input

        val radius = BlurConfig.coerceInMod(config.radius)
        val repeat = BlurConfig.coerceInMod(config.repeat)
        if (radius == 0 || repeat == 0) return input

        val rowBytes = width * 4
        val imageInfo = ImageInfo.makeN32Premul(width, height, input.imageInfo.colorInfo.colorSpace)
        val pixels = input.readPixels(dstInfo = imageInfo, dstRowBytes = rowBytes) ?: return input

        var blurred = pixels
        repeat(repeat) {
            blurred = Toolkit.blur(
                inputArray = blurred,
                vectorSize = 4,
                sizeX = width,
                sizeY = height,
                radius = radius,
                restriction = null
            )
        }

        val outputBitmap = Bitmap()
        if (!outputBitmap.installPixels(imageInfo, blurred, rowBytes)) return input
        outputBitmap.setImmutable()
        return outputBitmap
    }
}
