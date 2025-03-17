package com.korilin.akit.glide.extensions.ninepatch

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.LibraryGlideModule
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min

@GlideModule
class NinePatchLibraryGlideModule : LibraryGlideModule() {

    companion object {
        var registerCount: Int = 0
    }

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {

        registerCount++

        registry
            .prepend(
                InputStream::class.java,
                Drawable::class.java,
                NinePatchDrawableDecoder(context) { it },
            )
            .prepend(
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

