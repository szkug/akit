package munchkin.resources.runtime.glide.extensions.ninepatch

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import munchkin.graph.ninepatch.NinePatchChunk
import munchkin.graph.ninepatch.NinePatchType
import munchkin.graph.ninepatch.createNinePatchDrawable
import munchkin.graph.ninepatch.determineNinePatchType
import munchkin.graph.ninepatch.asNinePatchSource
import munchkin.resources.runtime.DefaultPlatformMunchkinLogger
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
        DefaultPlatformMunchkinLogger.debug("NinePatchDrawableDecoder") { "handles type=$type" }
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
        DefaultPlatformMunchkinLogger.debug("NinePatchDrawableDecoder") {
            "decode drawable=$drawable"
        }
        return SimpleResource(drawable)
    }
}
