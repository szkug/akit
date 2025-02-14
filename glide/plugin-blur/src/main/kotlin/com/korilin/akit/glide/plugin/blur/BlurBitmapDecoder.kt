package com.korilin.akit.glide.plugin.blur

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource

val BlurBitmapConfigOption = Option.memory<BlurConfig>(BlurBitmapDecoder::class.qualifiedName!!)

class BlurBitmapDecoder<Input : Any>(
    private val bitmapPool: BitmapPool,
    private val decoder: ResourceDecoder<Input, Bitmap>,
) : ResourceDecoder<Input, Bitmap> {

    override fun handles(source: Input, options: Options): Boolean {
        val enabled = options.get(BlurBitmapConfigOption) != null
        Log.d("BlurBitmapDecoder", "handle $enabled")
        return enabled && decoder.handles(source, options)
    }

    override fun decode(
        source: Input,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val config = options.get(BlurBitmapConfigOption)!!
        val bitmapResource = decoder.decode(source, width, height, options)
        return bitmapResource?.get()?.let {
            val blurBitmap = BlurToolkit.blur(config, it)
            BitmapResource.obtain(blurBitmap, bitmapPool)
        }
    }
}