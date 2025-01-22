package com.korilin.samples.compose.trace

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.module.AppGlideModule
import com.korilin.samples.compose.trace.ninepatch.NinePatchChunk
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min


class App : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}

@GlideModule
class NinePatchGlideModule : AppGlideModule() {

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
        registry
            .prepend(
                InputStream::class.java,
                Drawable::class.java,
                NinePatchDrawableDecoder(context) { it },
            ).prepend(
                ByteBuffer::class.java,
                Drawable::class.java,
                NinePatchDrawableDecoder(context) { ByteBufferBackedInputStream(it) },
            )
    }

}

class ByteBufferBackedInputStream(private var buffer: ByteBuffer) : InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }
        return buffer.get().toInt() and 0xFF
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }

        val readLen = min(len, buffer.remaining())
        buffer[bytes, off, readLen]
        return readLen
    }
}


class NinePatchDrawableDecoder<Input : Any>(
    private val context: Context,
    private val toStream: (Input) -> InputStream
) : ResourceDecoder<Input, Drawable> {

    companion object {
        val option = Option.memory(NinePatchDrawableDecoder::class.qualifiedName!!, false)
    }

    override fun handles(source: Input, options: Options): Boolean {
        val enable = options.get(option)!!
        if (!enable) return false
        val stream = toStream(source)
        val bitmap = BitmapFactory.decodeStream(stream)
        val ninepatch = NinePatchChunk.isRawNinePatchBitmap(bitmap)
        return ninepatch
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