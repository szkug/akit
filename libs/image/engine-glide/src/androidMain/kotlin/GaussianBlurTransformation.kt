package munchkin.image.glide.transformation

import android.graphics.Bitmap
import munchkin.graph.renderscript.BlurConfig
import munchkin.graph.renderscript.Toolkit
import munchkin.image.EngineContext

internal class GaussianBlurTransformation(
    private val config: BlurConfig,
) : BitmapTransformation() {

    override fun key(): String {
        return "munchkin.blur.${config.radius}.${config.repeat}"
    }

    override fun transform(
        context: EngineContext,
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
