package cn.szkug.akit.glide.extensions.ninepatch

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchType
import cn.szkug.akit.graph.ninepatch.createNinePatchDrawable
import cn.szkug.akit.graph.ninepatch.determineNinePatchType
import cn.szkug.akit.graph.ninepatch.asNinePatchSource
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
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
        val type = determineNinePatchType(bitmap.asNinePatchSource(), bitmap.ninePatchChunk)
        val isNinepatch = type == NinePatchType.Chunk || type == NinePatchType.Raw
        Log.d("NinePatchDrawableDecoder", "handles type=$type")
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
        val drawable = NinePatchChunk.createNinePatchDrawable(context, bitmap, null)
            ?: BitmapDrawable(context.resources, bitmap)
        Log.d("NinePatchDrawableDecoder", "decode drawable=$drawable")
        return SimpleResource(drawable)
    }
}
