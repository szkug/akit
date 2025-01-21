package com.korilin.akit.compose.image.transcoder

import android.content.Context
import android.graphics.Bitmap
import com.google.android.renderscript.BlurConfig
import com.google.android.renderscript.BlurToolkit
import com.korilin.akit.compose.image.publics.BitmapTranscoder

data class BlurTransformation(private val config: BlurConfig) : BitmapTranscoder() {

    override fun key(): String = "BlurTransformation.${config.radius}.${config.repeat}"

    override fun transcode(context: Context, resource: Bitmap, width: Int, height: Int): Bitmap {
        return BlurToolkit.blur(config, resource)
    }
}