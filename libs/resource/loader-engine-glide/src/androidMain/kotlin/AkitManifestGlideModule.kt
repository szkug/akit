package munchkin.resources.loader.glide.extensions

import android.content.Context
import munchkin.resources.loader.glide.extensions.lottie.LottieLibraryGlideModule
import munchkin.resources.loader.glide.extensions.ninepatch.NinePatchLibraryGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.module.GlideModule

class MunchkinManifestGlideModule : GlideModule {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // No-op for now.
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        LottieLibraryGlideModule().registerComponents(context, glide, registry)
        NinePatchLibraryGlideModule().registerComponents(context, glide, registry)
    }
}
