package cn.szkug.samples.compose.trace

import android.app.Application
import cn.szkug.akit.apps.cmp.Res
import cn.szkug.akit.image.AsyncImageLogger
import cn.szkug.akit.image.DefaultPlatformAsyncImageLogger
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    }
}
