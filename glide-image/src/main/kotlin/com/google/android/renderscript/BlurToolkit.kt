package com.google.android.renderscript

import android.graphics.Bitmap
import androidx.annotation.IntRange

data class BlurConfig(
    @IntRange(from = 0, to = MAX_MOD.toLong()) val radius: Int,
    @IntRange(from = 0, to = MAX_MOD.toLong()) val repeat: Int = radius,
) {
    companion object {
        private const val MAX_MOD = 25

        fun coerceInMod(mod: Int) = mod.coerceIn(0, MAX_MOD)
    }
}

object BlurToolkit {

    fun blur(config: BlurConfig, bitmap: Bitmap): Bitmap {
        val radius = BlurConfig.coerceInMod(config.radius)
        val repeat = BlurConfig.coerceInMod(config.repeat)
        if (radius == 0 || repeat == 0) return bitmap
        var blurred = bitmap
        repeat(repeat) {
            blurred = Toolkit.blur(blurred, radius = radius)
        }
        return blurred
    }
}