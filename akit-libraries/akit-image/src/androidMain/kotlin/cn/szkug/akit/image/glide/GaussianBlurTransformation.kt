package cn.szkug.akit.image.glide

import android.graphics.Bitmap
import cn.szkug.akit.graph.renderscript.BlurConfig
import cn.szkug.akit.graph.renderscript.Toolkit
import cn.szkug.akit.image.BitmapTransformation
import cn.szkug.akit.image.PlatformImageContext

internal class GaussianBlurTransformation(
    private val config: BlurConfig,
) : BitmapTransformation() {

    override fun key(): String {
        return "akit.blur.${config.radius}.${config.repeat}"
    }

    override fun transform(
        context: PlatformImageContext,
        resource: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val radius = BlurConfig.coerceInMod(config.radius)
        val repeat = BlurConfig.coerceInMod(config.repeat)
        if (radius == 0 || repeat == 0) return resource
        var blurred = resource
        repeat(repeat) {
            blurred = Toolkit.blur(blurred, radius = radius)
        }
        return blurred
    }
}
