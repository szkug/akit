package com.korilin.akit.glide.extensions.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.LazyBitmapDrawableResource
import java.security.MessageDigest

val BlurBitmapConfigOption = Option.memory<BlurConfig?>(BlurBitmapDecoder::class.qualifiedName!!)

data class BitmapDecoderContext(
    val context: Context,
    val bitmapPool: BitmapPool,
)
private typealias ResourceOutput<Output> = (Bitmap, BitmapDecoderContext) -> Resource<Output>?

class BlurBitmapDecoder<Input : Any, Output : Any>(
    private val context: BitmapDecoderContext,
    private val decoder: ResourceDecoder<Input, Bitmap>,
    private val resourceOutput: ResourceOutput<Output>
) : ResourceDecoder<Input, Output> {

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
    ): Resource<Output>? {
        val config = options.get(BlurBitmapConfigOption)!!
        val bitmapResource = decoder.decode(source, width, height, options)
        return bitmapResource?.get()?.let {
            val blurBitmap = BlurToolkit.blur(config, it)
            resourceOutput(blurBitmap, context)
        }
    }


    companion object {
        val BITMAP_OUTPUT: ResourceOutput<Bitmap> = { bitmap, context ->
            BitmapResource.obtain(bitmap, context.bitmapPool)
        }

        val BITMAP_DRAWABLE_OUTPUT: ResourceOutput<BitmapDrawable> = { bitmap, context ->
            val resource = BITMAP_OUTPUT.invoke(bitmap, context)
            LazyBitmapDrawableResource.obtain(context.context.resources, resource)
        }

        val DRAWABLE_OUTPUT: ResourceOutput<Drawable> = { bitmap, context ->
            BITMAP_DRAWABLE_OUTPUT.invoke(bitmap, context) as? Resource<Drawable>
        }
    }
}