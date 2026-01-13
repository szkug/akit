package cn.szkug.samples.compose.trace

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import cn.szkug.akit.image.glide.LargeBitmapLimitConfig
import cn.szkug.akit.image.glide.LargeBitmapLimitOption


@GlideModule
class GlideAppModuleImpl : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {

        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels

        builder.setDefaultRequestOptions(
            RequestOptions().format(DecodeFormat.PREFER_ARGB_8888).set(
                LargeBitmapLimitOption, LargeBitmapLimitConfig(widthPixels, heightPixels)
            )
        )
    }
}