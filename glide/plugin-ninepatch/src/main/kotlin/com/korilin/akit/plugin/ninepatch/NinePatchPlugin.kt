package com.korilin.akit.plugin.ninepatch

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.LibraryGlideModule
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class NinePatchLibraryGlideModule : LibraryGlideModule() {

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
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