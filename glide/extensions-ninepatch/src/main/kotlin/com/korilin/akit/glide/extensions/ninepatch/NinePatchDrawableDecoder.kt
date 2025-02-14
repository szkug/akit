package com.korilin.akit.glide.extensions.ninepatch

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import ua.anatolii.graphics.ninepatch.BitmapType
import ua.anatolii.graphics.ninepatch.NinePatchChunk
import java.io.InputStream

val NinepatchEnableOption = Option.memory(NinePatchDrawableDecoder::class.qualifiedName!!, false)

class NinePatchDrawableDecoder<Input : Any>(
    private val context: Context,
    private val toStream: (Input) -> InputStream
) : ResourceDecoder<Input, Drawable> {

    override fun handles(source: Input, options: Options): Boolean {
        val enable = options.get(NinepatchEnableOption)!!
        if (!enable) return false
        val stream = toStream(source)
        val bitmap = BitmapFactory.decodeStream(stream)
        val type = BitmapType.determineBitmapType(bitmap)
        val isNinepatch = type == BitmapType.NinePatch || type == BitmapType.RawNinePatch
        return isNinepatch
    }

    override fun decode(
        source: Input,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Drawable> {
        val stream = toStream(source)
        val bitmap = BitmapFactory.decodeStream(stream)
        return SimpleResource(NinePatchChunk.create9PatchDrawable(context, bitmap, null))
    }
}

