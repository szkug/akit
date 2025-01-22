package com.korilin.samples.compose.trace

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.bumptech.glide.module.LibraryGlideModule
import com.korilin.samples.compose.trace.ninepatch.NinePatchChunk

class App : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}

@GlideModule
class NinePatchGlideModule1 : LibraryGlideModule() {

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
        registry
            .prepend(Bitmap::class.java, Drawable::class.java, NinePatchDrawableTranscoder(context))
    }

}

class NinePatchDrawableTranscoder(private val context: Context) : ResourceDecoder<Bitmap?, Drawable> {

    override fun handles(source: Bitmap, options: Options): Boolean {
        return NinePatchChunk.isRawNinePatchBitmap(source)
    }

    override fun decode(
        source: Bitmap,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Drawable> {
        return SimpleResource(NinePatchChunk.create9PatchDrawable(context, source, null))
    }
}